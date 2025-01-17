------------------- MODULE DistributedTransactionProtocol -------------------
EXTENDS Naturals, Sequences, FiniteSets

CONSTANTS SERVERS, CLUSTER_TOOLS, RECOVERY_TOOLS

TransactionManagers == CLUSTER_TOOLS \union RECOVERY_TOOLS

Processes == SERVERS \union TransactionManagers

VARIABLES
    serverStates,
    clusterToolStates,
    recoveryToolStates,
    processMessageQueues,
    uuids

Messages ==
    [
        type : {"change-discover"},
        clusterTool : TransactionManagers
    ] \union
    [
        type: {"change-prepare"},
        clusterTool : CLUSTER_TOOLS, \* RECOVERY_TOOLS do not do prepares
        expectedMutativeMessageCount: Nat,
        change: Nat,
        version: Nat
    ] \union
    [
        type: {"change-commit"},
        clusterTool : TransactionManagers,
        expectedMutativeMessageCount: Nat
    ] \union
    [
        type: {"change-rollback"},
        clusterTool : TransactionManagers,
        expectedMutativeMessageCount: Nat
    ] \union
    [
        type: {"change-takeover"},
        clusterTool : RECOVERY_TOOLS, \* CLUSTER_TOOLS do not do takeovers
        expectedMutativeMessageCount: Nat
    ] \union
    [
        type : {"change-discover-response"},
        server: SERVERS,
        mode : {"ACCEPTING", "PREPARED"},
        mutativeMessageCount : Nat,
        latestChange : Nat,
        currentVersion : Nat,
        highestVersion : Nat,
        latestChangeState : { "", "COMMIT", "ROLLBACK" }
    ] \union
    [
        type: {"change-prepare-response", "change-commit-response", "change-rollback-response", "change-takeover-response"},
        server: SERVERS,
        action: {"ACCEPT", "REJECT"}
    ]

TypeOK ==
  /\ \A p \in Processes : processMessageQueues[p] \subseteq Messages
  /\ uuids /= {}


Init ==
    /\ serverStates = [s \in SERVERS |->
            [
                mode |-> "ACCEPTING",
                mutativeMessageCount |-> 0,
                latestChange |-> 0,
                currentVersion |-> 0,
                highestVersion |-> 0,
                commit |-> {},
                rollback |-> {}
            ]
        ]
    /\ clusterToolStates = [ct \in CLUSTER_TOOLS |->
            [
                mode |-> "START",
                changeUuid |-> 0,
                discoveries |-> {},
                discoveries2 |-> {},
                accepted |-> {},
                rejected |-> {},
                requireRollback |-> {}
            ]
        ]
    /\ recoveryToolStates = [rt \in RECOVERY_TOOLS |->
            [
                mode |-> "START",
                discoveries |-> {},
                discoveries2 |-> {},
                accepted |-> {},
                rejected |-> {},
                requireRollback |-> {},
                requireCommit |-> {}
            ]
        ]
    /\ processMessageQueues = [p \in Processes |-> {}]
    /\ uuids = 1..4

\* Operators for dealing with messages

SendMessageForProcess(queues, p, messagesByProcess) ==
    IF p \in DOMAIN messagesByProcess THEN queues[p] \union { messagesByProcess[p] }
    ELSE queues[p]

SendMessagesToProcesses(queues, messagesByProcess) == [ p \in Processes |-> SendMessageForProcess(queues, p, messagesByProcess) ]

SendMessageToAllServers(queues, message) == SendMessagesToProcesses(queues, [ s \in SERVERS |-> message ])

SendMessageToProcess(queues, p, message) == SendMessagesToProcesses(queues, [ x \in { p } |-> message ] )

RemoveMessage(queues, p, message) == [ queues EXCEPT ![p] = queues[p] \ {message} ]

HandleMessage(messageTarget, message, responseTarget, response) ==
    SendMessageToProcess(RemoveMessage(processMessageQueues, messageTarget, message), responseTarget, response)

FireIfMessage(states, process, messageType, operation(_,_)) ==
    /\ \E r \in processMessageQueues[process] : r.type = messageType
    /\ LET message == CHOOSE r \in processMessageQueues[process] : r.type = messageType
       IN operation(process, message)

FireIfMessageInMode(states, process, mode, messageType, operation(_,_)) ==
    /\ states[process].mode = mode
    /\ FireIfMessage(states, process, messageType, operation)

\* Operators for managing state

AddToElementField(array, element, name, value) == [ array EXCEPT ![element] = [array[element] EXCEPT ![name] = array[element][name] \union {value}] ]

ClearElementField(array, element, name) == [ array EXCEPT ![element] = [array[element] EXCEPT ![name] = {} ] ]

SetElementField(array, element, name, value) == [array EXCEPT ![element] = [array[element] EXCEPT ![name] = value]]

IncrementElementField(array, element, name) == SetElementField(array, element, name, array[element][name] + 1)

SetMode(element, mode, array) == SetElementField(array, element, "mode", mode)

SetLatestChange(element, change, array) == SetElementField(array, element, "latestChange", change)

SetCurrentVersion(element, version, array) == SetElementField(array, element, "currentVersion", version)

SetHighestVersion(element, version, array) == SetElementField(array, element, "highestVersion", version)

SetChangeUuid(element, uuid, array) == SetElementField(array, element, "changeUuid", uuid)

IncrementMutativeMessageCount(element, array) == IncrementElementField(array, element, "mutativeMessageCount")

AddDiscovery(array, element, message) == AddToElementField(array, element, "discoveries", message)

AddDiscovery2(array, element, message) == AddToElementField(array, element, "discoveries2", message)

AddCommit(element, commit, array) == AddToElementField(array, element, "commit", commit)

AddRollback(element, commit, array) == AddToElementField(array, element, "rollback", commit)

AddAccept(array, ct, s) == AddToElementField(array, ct, "accepted", s)

AddReject(array, ct, s) == AddToElementField(array, ct, "rejected", s)

SetRequireCommit(element, value, array) == SetElementField(array, element, "requireCommit", value)

SetRequireRollback(element, value, array) == SetElementField(array, element, "requireRollback", value)

ClearDiscoveries(element, array) == ClearElementField(array, element, "discoveries")

ClearAccepted(element, array) == ClearElementField(array, element, "accepted")

ClearRejected(element, array) == ClearElementField(array, element, "rejected")

ClearRequireRollback(element, array) == ClearElementField(array, element, "requireRollback")

\* Server operations

ServerReceiveDiscoverOp(s, message) ==
    LET
        changeState == CASE serverStates[s].latestChange \in serverStates[s].commit -> "COMMIT"
                         [] serverStates[s].latestChange \in serverStates[s].rollback -> "ROLLBACK"
                         [] OTHER -> ""
    IN
        /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-discover-response",
                    server |-> s,
                    mode |-> serverStates[s].mode,
                    mutativeMessageCount |-> serverStates[s].mutativeMessageCount,
                    latestChange |-> serverStates[s].latestChange,
                    currentVersion |-> serverStates[s].currentVersion,
                    highestVersion |-> serverStates[s].highestVersion,
                    latestChangeState |-> changeState
                ]
            )
        /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, uuids>>

ServerReceiveDiscover(s) == FireIfMessage(serverStates, s, "change-discover", ServerReceiveDiscoverOp)


ServerReceivePrepareOp(s, message) ==
    \/ /\ message.expectedMutativeMessageCount = serverStates[s].mutativeMessageCount
       /\ serverStates' =
            SetMode(s, "PREPARED",
                IncrementMutativeMessageCount(s,
                    SetLatestChange(s, message.change,
                        SetHighestVersion(s, message.version,
                            serverStates
                        )
                    )
                )
            )
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-prepare-response",
                    server |-> s,
                    action |-> "ACCEPT"
                ]
            )
       /\ UNCHANGED <<clusterToolStates, recoveryToolStates, uuids>>
    \/ /\ message.expectedMutativeMessageCount /= serverStates[s].mutativeMessageCount
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-prepare-response",
                    server |-> s,
                    action |-> "REJECT"
                ]
            )
       /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, uuids>>

ServerReceivePrepare(s) == FireIfMessage(serverStates, s, "change-prepare", ServerReceivePrepareOp)


ServerReceiveCommitOp(s, message) ==
    \/ /\ message.expectedMutativeMessageCount = serverStates[s].mutativeMessageCount
       /\ serverStates' =
            SetMode(s, "ACCEPTING",
                IncrementMutativeMessageCount(s,
                    SetCurrentVersion(s, serverStates[s].highestVersion,
                        AddCommit(s, serverStates[s].latestChange,
                            serverStates
                        )
                    )
                )
            )
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-commit-response",
                    server |-> s,
                    action |-> "ACCEPT"
                ]
            )
       /\ UNCHANGED <<clusterToolStates, recoveryToolStates, uuids>>
    \/ /\ message.expectedMutativeMessageCount /= serverStates[s].mutativeMessageCount
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-commit-response",
                    server |-> s,
                    action |-> "REJECT"
                ]
            )
       /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, uuids>>

ServerReceiveCommit(s) == FireIfMessage(serverStates, s, "change-commit", ServerReceiveCommitOp)


ServerReceiveRollbackOp(s, message) ==
    \/ /\ message.expectedMutativeMessageCount = serverStates[s].mutativeMessageCount
       /\ serverStates' =
            SetMode(s, "ACCEPTING",
                IncrementMutativeMessageCount(s,
                    AddRollback(s, serverStates[s].latestChange,
                        serverStates
                    )
                )
            )
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-rollback-response",
                    server |-> s,
                    action |-> "ACCEPT"
                ]
            )
       /\ UNCHANGED <<clusterToolStates, recoveryToolStates, uuids>>
    \/ /\ message.expectedMutativeMessageCount /= serverStates[s].mutativeMessageCount
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-rollback-response",
                    server |-> s,
                    action |-> "REJECT"
                ]
            )
       /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, uuids>>


ServerReceiveRollback(s) == FireIfMessage(serverStates, s, "change-rollback", ServerReceiveRollbackOp)


ServerReceiveTakeoverOp(s, message) ==
    \/ /\ message.expectedMutativeMessageCount = serverStates[s].mutativeMessageCount
       /\ serverStates' =
            IncrementMutativeMessageCount(s,
                serverStates
            )
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-takeover-response",
                    server |-> s,
                    action |-> "ACCEPT"
                ]
            )
       /\ UNCHANGED <<clusterToolStates, recoveryToolStates, uuids>>
    \/ /\ message.expectedMutativeMessageCount /= serverStates[s].mutativeMessageCount
       /\ processMessageQueues' = HandleMessage(s, message, message.clusterTool, [
                    type |-> "change-takeover-response",
                    server |-> s,
                    action |-> "REJECT"
                ]
            )
       /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, uuids>>

ServerReceiveTakeover(s) == FireIfMessage(serverStates, s, "change-takeover", ServerReceiveTakeoverOp)

\* Cluster tool operations

ClusterToolSendDiscover(ct) ==
    /\ clusterToolStates[ct].mode = "START"
    /\ clusterToolStates' = SetMode(ct, "DISCOVER", clusterToolStates)
    /\ processMessageQueues' = SendMessageToAllServers(processMessageQueues, [
                type |-> "change-discover",
                clusterTool |-> ct
            ]
        )
    /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolReceiveDiscoverResponseOp(ct, message) ==
    /\ clusterToolStates' = AddDiscovery(clusterToolStates, ct, message)
    /\ processMessageQueues' = RemoveMessage(processMessageQueues, ct, message)
    /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolReceiveDiscoverResponse(ct) == FireIfMessageInMode(clusterToolStates, ct, "DISCOVER", "change-discover-response", ClusterToolReceiveDiscoverResponseOp)

ClusterToolSendDiscover2(ct) ==
    /\ clusterToolStates[ct].mode = "DISCOVER"
    /\ LET
           acceptingDiscoveries == { discovery.server : discovery \in { discovery \in clusterToolStates[ct].discoveries : discovery.mode = "ACCEPTING" } }
       IN
            /\ acceptingDiscoveries = SERVERS
            /\ clusterToolStates' = SetMode(ct, "DISCOVER2", clusterToolStates)
            /\ processMessageQueues' = SendMessageToAllServers(processMessageQueues, [
                        type |-> "change-discover",
                        clusterTool |-> ct
                    ]
                )
            /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolReceiveDiscoverResponse2Op(ct, message) ==
    /\ clusterToolStates' = AddDiscovery2(clusterToolStates, ct, message)
    /\ processMessageQueues' = RemoveMessage(processMessageQueues, ct, message)
    /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolReceiveDiscoverResponse2(ct) == FireIfMessageInMode(clusterToolStates, ct, "DISCOVER2", "change-discover-response", ClusterToolReceiveDiscoverResponse2Op)

ClusterToolSendPrepare(ct) ==
    /\ clusterToolStates[ct].mode = "DISCOVER2"
    /\ clusterToolStates[ct].discoveries = clusterToolStates[ct].discoveries2
    /\ LET
           versions == { discovery.highestVersion : discovery \in clusterToolStates[ct].discoveries }
           maxVersion == CHOOSE version \in versions : \A otherVersion \in versions : otherVersion <= version
           newUuid == CHOOSE uuid \in uuids : TRUE
       IN
            /\ clusterToolStates' =
                SetMode(ct, "PREPARE",
                    SetChangeUuid(ct, newUuid,
                        clusterToolStates
                    )
                )
            /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in SERVERS |-> [
                            type |-> "change-prepare",
                            clusterTool |-> ct,
                            change |-> newUuid,
                            version |-> maxVersion + 1,
                            expectedMutativeMessageCount |-> (CHOOSE discovery \in clusterToolStates[ct].discoveries : discovery.server = s).mutativeMessageCount
                        ]
                    ]
                )
            /\ uuids' = uuids \ {newUuid}
            /\ UNCHANGED <<serverStates, recoveryToolStates>>

ClusterToolReceiveAcceptRejectOp(ct, message) ==
    \/ /\ message.action = "ACCEPT"
       /\ clusterToolStates' = AddAccept(clusterToolStates, ct, message.server)
       /\ processMessageQueues' = RemoveMessage(processMessageQueues, ct, message)
       /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>
    \/ /\ message.action = "REJECT"
       /\ clusterToolStates' = AddReject(clusterToolStates, ct, message.server)
       /\ processMessageQueues' = RemoveMessage(processMessageQueues, ct, message)
       /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolReceivePrepareResponse(ct) == FireIfMessageInMode(clusterToolStates, ct, "PREPARE", "change-prepare-response", ClusterToolReceiveAcceptRejectOp)

ClusterToolSendCommit(ct) ==
    /\ clusterToolStates[ct].mode = "PREPARE"
    /\ clusterToolStates[ct].accepted = SERVERS
    /\ clusterToolStates' =
        SetMode(ct, "COMMIT",
            ClearAccepted(ct,
                ClearRejected(ct,
                    clusterToolStates
                )
            )
        )
    /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in SERVERS |-> [
                    type |-> "change-commit",
                    clusterTool |-> ct,
                    expectedMutativeMessageCount |-> (CHOOSE discovery \in clusterToolStates[ct].discoveries : discovery.server = s).mutativeMessageCount + 1
                ]
            ]
        )
    /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolSendRollback(ct) ==
    /\ clusterToolStates[ct].mode = "PREPARE"
    /\ clusterToolStates[ct].accepted \union clusterToolStates[ct].rejected = SERVERS
    /\ clusterToolStates[ct].rejected /= {}
    /\ clusterToolStates' =  [clusterToolStates EXCEPT ![ct] = [
                mode |-> "ROLLBACK",
                changeUuid |-> clusterToolStates[ct].changeUuid,
                discoveries |-> clusterToolStates[ct].discoveries,
                accepted |-> {},
                rejected |-> {},
                requireRollback |-> clusterToolStates[ct].accepted
            ]
        ]
    /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in clusterToolStates[ct].accepted |-> [
                    type |-> "change-rollback",
                    clusterTool |-> ct,
                    expectedMutativeMessageCount |-> (CHOOSE discovery \in clusterToolStates[ct].discoveries : discovery.server = s).mutativeMessageCount + 1
                ]
            ]
        )
    /\ UNCHANGED <<serverStates, recoveryToolStates, uuids>>

ClusterToolReceiveCommitResponse(ct) == FireIfMessageInMode(clusterToolStates, ct, "COMMIT", "change-commit-response", ClusterToolReceiveAcceptRejectOp)

ClusterToolReceiveRollbackResponse(ct) == FireIfMessageInMode(clusterToolStates, ct, "ROLLBACK", "change-rollback-response", ClusterToolReceiveAcceptRejectOp)

ClusterToolCommitComplete(ct) ==
    /\ clusterToolStates[ct].mode = "COMMIT"
    /\ clusterToolStates[ct].accepted = SERVERS
    /\ clusterToolStates' = SetMode(ct, "COMMIT-COMPLETE", clusterToolStates)
    /\ UNCHANGED <<serverStates, recoveryToolStates, processMessageQueues, uuids>>

ClusterToolCommitFailComplete(ct) ==
    /\ clusterToolStates[ct].mode = "COMMIT"
    /\ clusterToolStates[ct].accepted \union clusterToolStates[ct].rejected = SERVERS
    /\ clusterToolStates[ct].rejected /= {}
    /\ clusterToolStates' = SetMode(ct, "COMMIT-FAIL-COMPLETE", clusterToolStates)
    /\ UNCHANGED <<serverStates, recoveryToolStates, processMessageQueues, uuids>>

ClusterToolRollbackComplete(ct) ==
    /\ clusterToolStates[ct].mode = "ROLLBACK"
    /\ clusterToolStates[ct].accepted = clusterToolStates[ct].requireRollback
    /\ clusterToolStates' = SetMode(ct, "ROLLBACK-COMPLETE", clusterToolStates)
    /\ UNCHANGED <<serverStates, recoveryToolStates, processMessageQueues, uuids>>

ClusterToolRollbackFailComplete(ct) ==
    /\ clusterToolStates[ct].mode = "ROLLBACK"
    /\ clusterToolStates[ct].accepted \union clusterToolStates[ct].rejected = clusterToolStates[ct].requireRollback
    /\ clusterToolStates[ct].rejected /= {}
    /\ clusterToolStates' = SetMode(ct, "ROLLBACK-FAIL-COMPLETE", clusterToolStates)
    /\ UNCHANGED <<serverStates, recoveryToolStates, processMessageQueues, uuids>>

ClusterToolStutterCompleted(ct) ==
    /\  \/ clusterToolStates[ct].mode = "COMMIT-COMPLETE"
        \/ clusterToolStates[ct].mode = "COMMIT-FAIL-COMPLETE"
        \/ clusterToolStates[ct].mode = "ROLLBACK-COMPLETE"
        \/ clusterToolStates[ct].mode = "ROLLBACK-FAIL-COMPLETE"
    /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, processMessageQueues, uuids>>

\* Recovery operations

RecoverySendDiscovery(rt) ==
    /\ recoveryToolStates[rt].mode = "START"
    /\ recoveryToolStates' = SetMode(rt, "DISCOVER", recoveryToolStates)
    /\ processMessageQueues' = SendMessageToAllServers(processMessageQueues, [
                type |-> "change-discover",
                clusterTool |-> rt
            ]
        )
    /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>

RecoveryReceiveDiscoverResponseOp(rt, message) ==
    /\ recoveryToolStates' = AddDiscovery(recoveryToolStates, rt, message)
    /\ processMessageQueues' = RemoveMessage(processMessageQueues, rt, message)
    /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>

RecoveryReceiveDiscoverResponse(rt) == FireIfMessageInMode(recoveryToolStates, rt, "DISCOVER", "change-discover-response", RecoveryReceiveDiscoverResponseOp)

RecoveryAssessTakeover(rt) ==
    /\ recoveryToolStates[rt].mode = "DISCOVER"
    /\ { discovery.server : discovery \in recoveryToolStates[rt].discoveries } = SERVERS
    /\ LET
           acceptingDiscoveries == { discovery.server : discovery \in { discovery \in recoveryToolStates[rt].discoveries : discovery.mode = "ACCEPTING" } }
       IN
            \/ acceptingDiscoveries = SERVERS
                /\ recoveryToolStates' = SetMode(rt, "DISCOVER2", recoveryToolStates)
                /\ processMessageQueues' = SendMessageToAllServers(processMessageQueues, [
                            type |-> "change-discover",
                            clusterTool |-> rt
                        ]
                    )
                /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>
            \/ acceptingDiscoveries /= SERVERS
                /\ recoveryToolStates' = SetMode(rt, "TAKEOVER", recoveryToolStates)
                /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in SERVERS |-> [
                                type |-> "change-takeover",
                                clusterTool |-> rt,
                                expectedMutativeMessageCount |-> (CHOOSE discovery \in recoveryToolStates[rt].discoveries : discovery.server = s).mutativeMessageCount
                            ]
                        ]
                    )
                /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>

RecoveryReceiveDiscoverResponse2Op(rt, message) ==
    /\ recoveryToolStates' = AddDiscovery2(recoveryToolStates, rt, message)
    /\ processMessageQueues' = RemoveMessage(processMessageQueues, rt, message)
    /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>

RecoveryReceiveDiscoverResponse2(rt) == FireIfMessageInMode(recoveryToolStates, rt, "DISCOVER2", "change-discover-response", RecoveryReceiveDiscoverResponse2Op)

RecoveryDiscover2Complete(rt) ==
    /\ recoveryToolStates[rt].mode = "DISCOVER2"
    /\ { discovery.server : discovery \in recoveryToolStates[rt].discoveries2 } = SERVERS
    /\  \/ recoveryToolStates[rt].discoveries = recoveryToolStates[rt].discoveries2
            /\ recoveryToolStates' = SetMode(rt, "RECOVERY-CONSISTENT-COMPLETE", recoveryToolStates)
            /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>
        \/ recoveryToolStates[rt].discoveries /= recoveryToolStates[rt].discoveries2
            /\ recoveryToolStates' = SetMode(rt, "RECOVERY-CONCURRENT-COMPLETE", recoveryToolStates)
            /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>

RecoveryReceiveAcceptRejectOp(rt, message) ==
    \/ /\ message.action = "ACCEPT"
       /\ recoveryToolStates' = AddAccept(recoveryToolStates, rt, message.server)
       /\ processMessageQueues' = RemoveMessage(processMessageQueues, rt, message)
       /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>
    \/ /\ message.action = "REJECT"
       /\ recoveryToolStates' = AddReject(recoveryToolStates, rt, message.server)
       /\ processMessageQueues' = RemoveMessage(processMessageQueues, rt, message)
       /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>

RecoveryReceiveTakeoverResponse(rt) ==
    FireIfMessageInMode(recoveryToolStates, rt, "TAKEOVER", "change-takeover-response", RecoveryReceiveAcceptRejectOp)

RecoveryTakeoverFailure(rt) ==
    /\ recoveryToolStates[rt].mode = "TAKEOVER"
    /\ recoveryToolStates[rt].accepted \union recoveryToolStates[rt].rejected = SERVERS
    /\ recoveryToolStates[rt].rejected /= {}
    /\ recoveryToolStates' = SetMode(rt, "RECOVERY-CONCURRENT-COMPLETE", recoveryToolStates)
    /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>

RecoveryTakeoverSuccess(rt) ==
    /\ recoveryToolStates[rt].mode = "TAKEOVER"
    /\ recoveryToolStates[rt].accepted = SERVERS
    /\
        LET
            latestChanges == { discovery.latestChange : discovery \in recoveryToolStates[rt].discoveries }
            preparedDiscoveries == { discovery.server : discovery \in { discovery \in recoveryToolStates[rt].discoveries : discovery.mode = "PREPARED" } }
            changeStates == { discovery.latestChangeState : discovery \in { discovery \in recoveryToolStates[rt].discoveries : discovery.mode = "ACCEPTING" /\ discovery.latestChangeState \in { "COMMIT", "ROLLBACK" } } }
        IN
            \/ Cardinality(latestChanges) = 1
                 /\ IF
                        "COMMIT" \in changeStates
                    THEN
                        /\ recoveryToolStates' =
                            SetMode(rt, "TAKEOVER-COMMIT",
                                ClearAccepted(rt,
                                    ClearRejected(rt,
                                        SetRequireCommit(rt, preparedDiscoveries,
                                            recoveryToolStates
                                        )
                                    )
                                )
                            )
                        /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in preparedDiscoveries |-> [
                                        type |-> "change-commit",
                                        clusterTool |-> rt,
                                        expectedMutativeMessageCount |-> (CHOOSE discovery \in recoveryToolStates[rt].discoveries : discovery.server = s).mutativeMessageCount + 1
                                    ]
                                ]
                            )
                    ELSE
                        /\ recoveryToolStates' =
                            SetMode(rt, "TAKEOVER-ROLLBACK",
                                ClearAccepted(rt,
                                    ClearRejected(rt,
                                        SetRequireRollback(rt, preparedDiscoveries,
                                            recoveryToolStates
                                        )
                                    )
                                )
                            )
                        /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in preparedDiscoveries |-> [
                                        type |-> "change-rollback",
                                        clusterTool |-> rt,
                                        expectedMutativeMessageCount |-> (CHOOSE discovery \in recoveryToolStates[rt].discoveries : discovery.server = s).mutativeMessageCount + 1
                                    ]
                                ]
                            )
            \/ Cardinality(latestChanges) /= 1
                /\ recoveryToolStates' =
                    SetMode(rt, "TAKEOVER-ROLLBACK",
                        ClearAccepted(rt,
                            ClearRejected(rt,
                                recoveryToolStates
                            )
                        )
                    )
                /\ processMessageQueues' = SendMessagesToProcesses(processMessageQueues, [ s \in preparedDiscoveries |-> [
                                type |-> "change-rollback",
                                clusterTool |-> rt,
                                expectedMutativeMessageCount |-> (CHOOSE discovery \in recoveryToolStates[rt].discoveries : discovery.server = s).mutativeMessageCount + 1
                            ]
                        ]
                    )
    /\ UNCHANGED <<serverStates, clusterToolStates, uuids>>

RecoveryReceiveCommitResponse(rt) ==
    FireIfMessageInMode(recoveryToolStates, rt, "TAKEOVER-COMMIT", "change-commit-response", RecoveryReceiveAcceptRejectOp)

RecoveryReceiveRollbackResponse(rt) ==
    FireIfMessageInMode(recoveryToolStates, rt, "TAKEOVER-ROLLBACK", "change-rollback-response", RecoveryReceiveAcceptRejectOp)

RecoveryCommitComplete(rt) ==
    /\ recoveryToolStates[rt].mode = "TAKEOVER-COMMIT"
    /\ recoveryToolStates[rt].accepted = recoveryToolStates[rt].requireCommit
    /\ recoveryToolStates' = SetMode(rt, "RECOVERY-COMMIT-COMPLETE", recoveryToolStates)
    /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>

RecoveryCommitFailComplete(rt) ==
    /\ recoveryToolStates[rt].mode = "TAKEOVER-COMMIT"
    /\ recoveryToolStates[rt].accepted \union recoveryToolStates[rt].rejected = recoveryToolStates[rt].requireCommit
    /\ recoveryToolStates[rt].rejected /= {}
    /\ recoveryToolStates' = SetMode(rt, "RECOVERY-COMMIT-FAIL-COMPLETE", recoveryToolStates)
    /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>

RecoveryRollbackComplete(rt) ==
    /\ recoveryToolStates[rt].mode = "TAKEOVER-ROLLBACK"
    /\ recoveryToolStates[rt].accepted = recoveryToolStates[rt].requireRollback
    /\ recoveryToolStates' = SetMode(rt, "RECOVERY-ROLLBACK-COMPLETE", recoveryToolStates)
    /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>

RecoveryRollbackFailComplete(rt) ==
    /\ recoveryToolStates[rt].mode = "TAKEOVER-ROLLBACK"
    /\ recoveryToolStates[rt].accepted \union recoveryToolStates[rt].rejected = recoveryToolStates[rt].requireRollback
    /\ recoveryToolStates[rt].rejected /= {}
    /\ recoveryToolStates' = SetMode(rt, "RECOVERY-ROLLBACK-FAIL-COMPLETE", recoveryToolStates)
    /\ UNCHANGED <<serverStates, clusterToolStates, processMessageQueues, uuids>>

RecoveryStutterCompleted(rt) ==
    /\ UNION({ processMessageQueues[p] : p \in Processes }) = {}
    /\  \/ recoveryToolStates[rt].mode = "RECOVERY-CONSISTENT-COMPLETE"
        \/ recoveryToolStates[rt].mode = "RECOVERY-CONCURRENT-COMPLETE"
        \/ recoveryToolStates[rt].mode = "RECOVERY-COMMIT-COMPLETE"
        \/ recoveryToolStates[rt].mode = "RECOVERY-COMMIT-FAIL-COMPLETE"
        \/ recoveryToolStates[rt].mode = "RECOVERY-ROLLBACK-FAIL-COMPLETE"
    /\ UNCHANGED <<serverStates, clusterToolStates, recoveryToolStates, processMessageQueues, uuids>>


Next ==
    \/ \E s \in SERVERS :
        \/ ServerReceiveDiscover(s)
        \/ ServerReceivePrepare(s)
        \/ ServerReceiveCommit(s)
        \/ ServerReceiveRollback(s)
        \/ ServerReceiveTakeover(s)
    \/ \E ct \in CLUSTER_TOOLS :
        \/ ClusterToolSendDiscover(ct)
        \/ ClusterToolReceiveDiscoverResponse(ct)
        \/ ClusterToolSendDiscover2(ct)
        \/ ClusterToolReceiveDiscoverResponse2(ct)
        \/ ClusterToolSendPrepare(ct)
        \/ ClusterToolReceivePrepareResponse(ct)
        \/ ClusterToolSendCommit(ct)
        \/ ClusterToolSendRollback(ct)
        \/ ClusterToolReceiveCommitResponse(ct)
        \/ ClusterToolReceiveRollbackResponse(ct)
        \/ ClusterToolCommitComplete(ct)
        \/ ClusterToolCommitFailComplete(ct)
        \/ ClusterToolRollbackComplete(ct)
        \/ ClusterToolRollbackFailComplete(ct)
        \/ ClusterToolStutterCompleted(ct)
    \/ \E rt \in RECOVERY_TOOLS :
        \/ RecoverySendDiscovery(rt)
        \/ RecoveryReceiveDiscoverResponse(rt)
        \/ RecoveryAssessTakeover(rt)
        \/ RecoveryReceiveDiscoverResponse2(rt)
        \/ RecoveryDiscover2Complete(rt)
        \/ RecoveryReceiveTakeoverResponse(rt)
        \/ RecoveryTakeoverFailure(rt)
        \/ RecoveryTakeoverSuccess(rt)
        \/ RecoveryReceiveCommitResponse(rt)
        \/ RecoveryReceiveRollbackResponse(rt)
        \/ RecoveryCommitComplete(rt)
        \/ RecoveryCommitFailComplete(rt)
        \/ RecoveryRollbackComplete(rt)
        \/ RecoveryRollbackFailComplete(rt)
        \/ RecoveryStutterCompleted(rt)

Invariant ==
    LET
        allCommittedChanges == UNION { serverStates[s].commit : s \in SERVERS }
        allRolledbackChanges == UNION { serverStates[s].rollback : s \in SERVERS }
        serversWithCommit == [commit \in allCommittedChanges |-> { s \in SERVERS : commit \in serverStates[s].commit} ]
        commitsOnAllServers == { commit \in allCommittedChanges : serversWithCommit[commit] = SERVERS }
        clusterToolCommits == { clusterToolStates[ct].changeUuid : ct \in { ct \in CLUSTER_TOOLS : clusterToolStates[ct].mode = "COMMIT-COMPLETE" } }
    IN
        /\ TypeOK
        /\ allCommittedChanges \intersect allRolledbackChanges = {}
        /\ clusterToolCommits \subseteq allCommittedChanges
        /\ clusterToolCommits \subseteq commitsOnAllServers


=============================================================================
\* Modification History
\* Last modified Tue Nov 20 10:17:46 GMT 2018 by CGRE
\* Created Mon Nov 05 12:59:25 GMT 2018 by CGRE
