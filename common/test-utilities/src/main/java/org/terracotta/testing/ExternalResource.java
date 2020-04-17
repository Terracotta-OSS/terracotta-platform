package org.terracotta.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import static java.util.Arrays.asList;

/**
 * A base class for Rules (like TemporaryFolder) that set up an external
 * resource before a test (a file, socket, server, database connection, etc.),
 * and guarantee to tear it down afterward:
 *
 * <pre>
 * public static class UsesExternalResource {
 *  Server myServer= new Server();
 *
 *  &#064;Rule
 *  public ExternalResource resource= new ExternalResource() {
 *      &#064;Override
 *      protected void before() throws Throwable {
 *          myServer.connect();
 *         };
 *
 *      &#064;Override
 *      protected void after() {
 *          myServer.disconnect();
 *         };
 *     };
 *
 *  &#064;Test
 *  public void testFoo() {
 *      new Client().run(myServer);
 *     }
 * }
 * </pre>
 *
 * @since 4.7
 */
public abstract class ExternalResource implements TestRule {
  public Statement apply(Statement base, Description description) {
    return statement(base);
  }

  private Statement statement(final Statement base) {
    // Note: Current Junit's ExternalResource skeleton is hiding any test exception in case an exception is thrown from after()
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        before();
        Throwable t = null;
        try {
          base.evaluate();
        } catch (Throwable t1) {
          t = t1;
        } finally {
          try {
            after();
          } catch (Throwable t2) {
            t = t == null ? t2 : new MultipleFailureException(asList(t, t2));
          }
        }
        if (t != null) {
          throw t;
        }
      }
    };
  }

  /**
   * Override to set up your specific external resource.
   *
   * @throws Throwable if setup fails (which will disable {@code after}
   */
  protected void before() throws Throwable {
    // do nothing
  }

  /**
   * Override to tear down your specific external resource.
   */
  protected void after() throws Throwable {
    // do nothing
  }
}