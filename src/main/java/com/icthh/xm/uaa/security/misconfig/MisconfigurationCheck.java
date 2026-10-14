package com.icthh.xm.uaa.security.misconfig;

/**
 * A single security misconfiguration check evaluated by {@link MisconfigurationMonitor}.
 * <p>
 * Implementations are auto-discovered as Spring beans. At startup the monitor runs every check once; each check
 * that reports a misconfiguration is then re-run on a fixed interval and its message is logged at {@code ERROR}
 * until it reports the problem is gone. Checks must be cheap and side-effect free (reads only).
 */
public interface MisconfigurationCheck {

    /**
     * Evaluate the current configuration.
     *
     * @return {@link Result#misconfigured(String)} with an actionable message while the problem is present,
     *         or {@link Result#ok()} once it is resolved.
     */
    Result check();

    /**
     * Short name for logging (defaults to the implementation class simple name).
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Outcome of a check: whether the configuration is still misconfigured and the message to log if so.
     */
    final class Result {

        private static final Result OK = new Result(false, null);

        private final boolean misconfigured;
        private final String message;

        private Result(boolean misconfigured, String message) {
            this.misconfigured = misconfigured;
            this.message = message;
        }

        public static Result ok() {
            return OK;
        }

        public static Result misconfigured(String message) {
            return new Result(true, message);
        }

        public boolean isMisconfigured() {
            return misconfigured;
        }

        public String getMessage() {
            return message;
        }
    }
}
