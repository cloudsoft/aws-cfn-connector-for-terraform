package io.cloudsoft.terraform.infrastructure;

/** Unchecked exceptions indicating a handler request should fail.
 * These are used primarily to ensure an appropriate level of logging to the consumer.
 */
public class ConnectorHandlerFailures {

    public static RuntimeException handled(String message) {
        return new Handled(message);
    }
    public static RuntimeException unhandled(String message) {
        return new Unhandled(message);
    }
    public static RuntimeException unhandled(String message, Throwable cause) {
        return new Unhandled(message, cause);
    }

    /** An exception which has been handled and logged, with a nice message supplied here.
     * The catcher should not do any further logging, but can include the message as a reason
     * why the failure occurred.
     */
    public static class Handled extends RuntimeException {
        private static final long serialVersionUID = -6582312522891789442L;
        protected Handled(String message) {
            super(message);
        }
    }

    /** An exception which is expected, and has a nice message, but which has not
     * been handled or logged. The catcher should log, and if a cause is supplied should
     * log its trace, and can include the message as a reason why the failure occurred.
     */
    public static class Unhandled extends RuntimeException {
        private static final long serialVersionUID = -4661749698259957836L;
        public Unhandled(String message) {
            super(message);
        }
        protected Unhandled(String message, Throwable cause) {
            super(message, cause);
        }
    }


}
