package com.dokany.java;

import org.jetbrains.annotations.NotNull;

import com.dokany.java.constants.MountError;
import com.dokany.java.structure.DeviceOptions;
import com.sun.jna.WString;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Main class to start and stop Dokany file system.
 *
 */
@Slf4j
public final class DokanyDriver {
	@NonNull
	private final DokanyFileSystem fileSystem;
	@NonNull
	private final DeviceOptions deviceOptions;

	public DokanyDriver(@NonNull final DeviceOptions deviceOptions, @NonNull final DokanyFileSystem fileSystem) {

		this.deviceOptions = deviceOptions;
		this.fileSystem = fileSystem;

		log.info("Dokany version: {}", getVersion());
		log.info("Dokany driver version: {}", getDriverVersion());
	}

	/**
	 * Get driver version.
	 *
	 * @see {@link NativeMethods#DokanDriverVersion()}
	 *
	 * @return
	 */
	public final long getDriverVersion() {
		return NativeMethods.DokanDriverVersion();
	}

	/**
	 * Get Dokany version.
	 *
	 * @see {@link NativeMethods#DokanVersion()}
	 * @return
	 */
	public final long getVersion() {
		return NativeMethods.DokanVersion();
	}

	/**
	 * Get file system.
	 *
	 * @return
	 */
	@NotNull
	public final DokanyFileSystem getFileSystem() {
		return fileSystem;
	}

	/**
	 * Calls {@link com.dokany.java.NativeMethods#DokanMain(DeviceOptions, Operations)}. Has {@link java.lang.Runtime#addShutdownHook(Thread)} which calls {@link #shutdown()}
	 */
	public final void start() {
		try {
			final int mountStatus = NativeMethods.DokanMain(deviceOptions, new DokanyOperationsProxy(fileSystem));

			if (mountStatus < 0) {
				throw new IllegalStateException(MountError.fromInt(mountStatus).description());
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					shutdown();
				}
			});
		} catch (final Throwable t) {
			log.warn("Error mounting", t);
			throw t;
		}
	}

	/**
	 * Calls {@link #stop(String)}.
	 */
	public final void shutdown() {
		stop(deviceOptions.MountPoint.toString());
	}

	/**
	 * Calls {@link NativeMethods#DokanUnmount(char)} and {@link NativeMethods#DokanRemoveMountPoint(WString)}
	 *
	 * @param mountPoint
	 */
	public final static void stop(@NotNull final String mountPoint) {
		log.info("Unmount and shutdown: {}", mountPoint);
		NativeMethods.DokanUnmount(mountPoint.charAt(0));
		NativeMethods.DokanRemoveMountPoint(new WString(mountPoint));
	}
}
