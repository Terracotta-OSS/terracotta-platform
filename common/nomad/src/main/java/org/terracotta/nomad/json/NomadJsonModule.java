/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.nomad.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RejectionReason;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;

/**
 * @author Mathieu Carbou
 */
public class NomadJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public NomadJsonModule() {
    super(NomadJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(NomadChange.class, NomadChangeMixin.class);

    setMixInAnnotation(MutativeMessage.class, MutativeMessageMixin.class);
    setMixInAnnotation(AcceptRejectResponse.class, AcceptRejectResponseMixin.class);
    setMixInAnnotation(ChangeDetails.class, ChangeDetailsMixin.class);
    setMixInAnnotation(CommitMessage.class, CommitMessageMixin.class);
    setMixInAnnotation(DiscoverResponse.class, DiscoverResponseMixin.class);
    setMixInAnnotation(PrepareMessage.class, PrepareMessageMixin.class);
    setMixInAnnotation(RollbackMessage.class, RollbackMessageMixin.class);
    setMixInAnnotation(TakeoverMessage.class, TakeoverMessageMixin.class);

    setMixInAnnotation(ChangeRequest.class, ChangeRequestMixin.class);
    setMixInAnnotation(NomadChangeInfo.class, NomadChangeInfoMixin.class);
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
  public interface NomadChangeMixin extends NomadChange {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    String getSummary();
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return singletonList(new JavaTimeModule());
  }

  public static class AcceptRejectResponseMixin extends AcceptRejectResponse {
    @JsonCreator
    protected AcceptRejectResponseMixin(@JsonProperty(value = "accepted", required = true) boolean accepted,
                                        @JsonProperty(value = "rejectionReason") RejectionReason rejectionReason,
                                        @JsonProperty(value = "rejectionMessage") String rejectionMessage,
                                        @JsonProperty(value = "lastMutationHost") String lastMutationHost,
                                        @JsonProperty(value = "lastMutationUser") String lastMutationUser) {
      super(accepted, rejectionReason, rejectionMessage, lastMutationHost, lastMutationUser);
    }

    @Override
    @JsonIgnore
    public boolean isRejected() {
      return super.isRejected();
    }
  }

  @SuppressWarnings("unused")
  public static class ChangeDetailsMixin<T> extends ChangeDetails<T> {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final T result;

    @JsonCreator
    public ChangeDetailsMixin(@JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                              @JsonProperty(value = "state", required = true) ChangeRequestState state,
                              @JsonProperty(value = "version", required = true) long version,
                              @JsonProperty(value = "operation", required = true) NomadChange operation,
                              @JsonProperty(value = "result") T result,
                              @JsonProperty(value = "creationHost", required = true) String creationHost,
                              @JsonProperty(value = "creationUser", required = true) String creationUser,
                              @JsonProperty(value = "creationTimestamp", required = true) Instant creationTimestamp) {
      super(changeUuid, state, version, operation, result, creationHost, creationUser, creationTimestamp);
      this.result = result;
    }
  }

  public static class CommitMessageMixin extends CommitMessage {
    @JsonCreator
    public CommitMessageMixin(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                              @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                              @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                              @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp,
                              @JsonProperty(value = "changeUuid", required = true) UUID changeUuid) {
      super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp, changeUuid);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
  @JsonSubTypes({
      @JsonSubTypes.Type(name = "COMMIT", value = CommitMessage.class),
      @JsonSubTypes.Type(name = "PREPARE", value = PrepareMessage.class),
      @JsonSubTypes.Type(name = "TAKEOVER", value = TakeoverMessage.class),
      @JsonSubTypes.Type(name = "TAKEOVER", value = TakeoverMessage.class),
  })
  public static abstract class MutativeMessageMixin extends MutativeMessage {
    @JsonCreator
    MutativeMessageMixin(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                         @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                         @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                         @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp) {
      super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp);
    }
  }

  public static class DiscoverResponseMixin<T> extends DiscoverResponse<T> {
    @JsonCreator
    public DiscoverResponseMixin(@JsonProperty(value = "mode", required = true) NomadServerMode mode,
                                 @JsonProperty(value = "mutativeMessageCount", required = true) long mutativeMessageCount,
                                 @JsonProperty(value = "lastMutationHost") String lastMutationHost,
                                 @JsonProperty(value = "lastMutationUser") String lastMutationUser,
                                 @JsonProperty(value = "lastMutationTimestamp") Instant lastMutationTimestamp,
                                 @JsonProperty(value = "currentVersion", required = true) long currentVersion,
                                 @JsonProperty(value = "highestVersion", required = true) long highestVersion,
                                 @JsonProperty(value = "latestChange") ChangeDetails<T> latestChange,
                                 @JsonProperty(value = "checkpoints") List<NomadChangeInfo> checkpoints) {
      super(mode, mutativeMessageCount, lastMutationHost, lastMutationUser, lastMutationTimestamp, currentVersion, highestVersion, latestChange, checkpoints);
    }
  }

  public static class PrepareMessageMixin extends PrepareMessage {
    @JsonCreator
    public PrepareMessageMixin(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                               @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                               @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                               @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp,
                               @JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                               @JsonProperty(value = "versionNumber", required = true) long versionNumber,
                               @JsonProperty(value = "change") NomadChange change) {
      super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp, changeUuid, versionNumber, change);
    }
  }

  public static class RollbackMessageMixin extends RollbackMessage {
    @JsonCreator
    public RollbackMessageMixin(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                                @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                                @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                                @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp,
                                @JsonProperty(value = "changeUuid", required = true) UUID changeUuid) {
      super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp, changeUuid);
    }
  }

  public static class TakeoverMessageMixin extends TakeoverMessage {
    @JsonCreator
    public TakeoverMessageMixin(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                                @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                                @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                                @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp) {
      super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp);
    }
  }

  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  public static class ChangeRequestMixin<T> extends ChangeRequest<T> {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final T changeResult;

    public ChangeRequestMixin(ChangeRequestState state, long version, String prevChangeId, NomadChange change, T changeResult, String creationHost, String creationUser, Instant creationTimestamp) {
      super(state, version, prevChangeId, change, changeResult, creationHost, creationUser, creationTimestamp);
      this.changeResult = changeResult;
    }
  }

  public static class NomadChangeInfoMixin extends NomadChangeInfo {
    @JsonCreator
    public NomadChangeInfoMixin(@JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                                @JsonProperty(value = "nomadChange", required = true) NomadChange nomadChange,
                                @JsonProperty(value = "changeRequestState", required = true) ChangeRequestState changeRequestState,
                                @JsonProperty(value = "version", required = true) long version,
                                @JsonProperty(value = "creationHost", required = true) String creationHost,
                                @JsonProperty(value = "creationUser", required = true) String creationUser,
                                @JsonProperty(value = "creationTimestamp", required = true) Instant creationTimestamp) {
      super(changeUuid, nomadChange, changeRequestState, version, creationHost, creationUser, creationTimestamp);
    }
  }
}
