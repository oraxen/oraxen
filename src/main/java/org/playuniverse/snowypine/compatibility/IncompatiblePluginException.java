package org.playuniverse.snowypine.compatibility;

import org.playuniverse.snowypine.utils.plugin.PluginPackage;

public class IncompatiblePluginException extends RuntimeException {

	static final long serialVersionUID = -3387516993124229948L;

	/**
	 * Constructs a new IncompatiblePluginException with {@code null} as its detail
	 * message. The cause is not initialized, and may subsequently be initialized by
	 * a call to {@link #initCause}.
	 */
	public IncompatiblePluginException() {
		super();
	}

	/**
	 * Constructs a new IncompatiblePluginException with the specified detail
	 * message. The cause is not initialized, and may subsequently be initialized by
	 * a call to {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for later
	 *                retrieval by the {@link #getMessage()} method.
	 */
	public IncompatiblePluginException(String message) {
		super(message);
	}

	/**
	 * Constructs a new IncompatiblePluginException with the specified
	 * {@code PluginPackage} and compatible versions.
	 *
	 * @param pluginPackage the
	 *                      {@link com.syntaxphoenix.utilities.plugin.PluginPackage}
	 *                      which is used to build the detailed message
	 * @param versions      the compatible versions of this plugin
	 */
	public IncompatiblePluginException(PluginPackage pluginPackage, String... versions) {
		super(pluginPackage.getName() + " is not compatible with the " /*+ Tracker.getClassFromStack(1).map(Class::getName).orElse(null)*/
			+ " addon. Please use one of the following versions instead: " + String.join(", ", versions));
	}

	/**
	 * Constructs a new IncompatiblePluginException with the specified detail
	 * message and cause.
	 * <p>
	 * Note that the detail message associated with {@code cause} is <i>not</i>
	 * automatically incorporated in this IncompatiblePluginException's detail
	 * message.
	 *
	 * @param message the detail message (which is saved for later retrieval by the
	 *                {@link #getMessage()} method).
	 * @param cause   the cause (which is saved for later retrieval by the
	 *                {@link #getCause()} method). (A <tt>null</tt> value is
	 *                permitted, and indicates that the cause is nonexistent or
	 *                unknown.)
	 * @since 1.4
	 */
	public IncompatiblePluginException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new IncompatiblePluginException with the specified cause and a
	 * detail message of <tt>(cause==null ? null : cause.toString())</tt> (which
	 * typically contains the class and detail message of <tt>cause</tt>). This
	 * constructor is useful for IncompatiblePluginExceptions that are little more
	 * than wrappers for other throwables (for example,
	 * {@link java.security.PrivilegedActionException}).
	 *
	 * @param cause the cause (which is saved for later retrieval by the
	 *              {@link #getCause()} method). (A <tt>null</tt> value is
	 *              permitted, and indicates that the cause is nonexistent or
	 *              unknown.)
	 * @since 1.4
	 */
	public IncompatiblePluginException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new IncompatiblePluginException with the specified detail
	 * message, cause, suppression enabled or disabled, and writable stack trace
	 * enabled or disabled.
	 *
	 * @param message            the detail message.
	 * @param cause              the cause. (A {@code null} value is permitted, and
	 *                           indicates that the cause is nonexistent or
	 *                           unknown.)
	 * @param enableSuppression  whether or not suppression is enabled or disabled
	 * @param writableStackTrace whether or not the stack trace should be writable
	 * @since 1.7
	 */
	protected IncompatiblePluginException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
