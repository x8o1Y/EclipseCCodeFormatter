/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Ben Konrath <ben@bagu.org> - initial implementation
 *     Red Hat Incorporated - improvements based on comments from JDT developers
 *     IBM Corporation - Code review and integration
 *     IBM Corporation - Fix for 340181
 *******************************************************************************/

/**
 * @author alexeynskvortsov@gmail.com
 * @file This utility is designed to format the C/C++ source code using the formatting tool built into eclipse CDT (CCodeFormatter). 
 */
/**
 * This file is derived from org.eclipse.jdt.core/formatter/org/eclipse/jdt/core/formatter/CodeFormatterApplication.java
 * 
 */

//package org.eclipse.jdt.core.formatter;
package org.eclipse.cdt.utils.CCodeFormatter;

import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.cdt.internal.core.CharOperation;
import org.eclipse.cdt.internal.formatter.CCodeFormatter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.cdt.core.ToolFactory;
//import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;

public class CCodeFormatterApp {
	/**
	 * Deals with the messages in the properties file (cut n' pasted from a
	 * generated class).
	 */
	private final static class Messages extends NLS {
		//private static final String BUNDLE_NAME = "org.eclipse.jdt.core.formatter.messages";//$NON-NLS-1$
		private static final String BUNDLE_NAME = "org.eclipse.cdt.utils.CCodeFormatter.messages";//$NON-NLS-1$

		public static String CommandLineConfigFile;

		public static String CommandLineDone;

		public static String CommandLineErrorConfig;

		public static String CommandLineErrorFileTryFullPath;

		public static String CommandLineErrorFile;

		public static String CommandLineErrorFileDir;

		public static String CommandLineErrorQuietVerbose;

		public static String CommandLineErrorNoConfigFile;

		public static String CommandLineFormatting;

		public static String CommandLineStart;

		public static String CommandLineUsage;

		public static String ConfigFileNotFoundErrorTryFullPath;

		public static String ConfigFileReadingError;

		public static String FormatProblem;

		public static String CaughtException;

		public static String ExceptionSkip;

		static {
			NLS.initializeMessages(BUNDLE_NAME, Messages.class);
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @return the manipulated String
		 */
		public static String bind(String message) {
			return bind(message, null);
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @param binding
		 *            the object to be inserted into the message
		 * @return the manipulated String
		 */
		public static String bind(String message, Object binding) {
			return bind(message, new Object[] {
				binding
			});
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @param binding1
		 *            An object to be inserted into the message
		 * @param binding2
		 *            A second object to be inserted into the message
		 * @return the manipulated String
		 */
		public static String bind(String message, Object binding1, Object binding2) {
			return bind(message, new Object[] {
					binding1, binding2
			});
		}

		/**
		 * Bind the given message's substitution locations with the given string
		 * values.
		 *
		 * @param message
		 *            the message to be manipulated
		 * @param bindings
		 *            An array of objects to be inserted into the message
		 * @return the manipulated String
		 */
		public static String bind(String message, Object[] bindings) {
			return MessageFormat.format(message, bindings);
		}
	}

	// private static char[][] JAVA_LIKE_EXTENSIONS;
	private static final Set<String> supportedExtensions=new HashSet<String>();
	
	private static final String ARG_CONFIG = "-config"; //$NON-NLS-1$

	private static final String ARG_HELP = "-help"; //$NON-NLS-1$

	private static final String ARG_QUIET = "-quiet"; //$NON-NLS-1$

	private static final String ARG_VERBOSE = "-verbose"; //$NON-NLS-1$

	private String configName;

	private Map options = null;

	private static final String PDE_LAUNCH = "-pdelaunch"; //$NON-NLS-1$

	private boolean quiet = false;

	private boolean verbose = false;

	private static final int DEFAULT_READING_SIZE = 8192;
	private static final int DEFAULT_WRITING_SIZE = 1024;
	public final static String UTF_8 = "UTF-8";	//$NON-NLS-1$
		
	static
	{
		supportedExtensions.add("c"); //$NON-NLS-1$
		supportedExtensions.add("C"); //$NON-NLS-1$
		supportedExtensions.add("cc"); //$NON-NLS-1$
		supportedExtensions.add("CC"); //$NON-NLS-1$
		supportedExtensions.add("cpp"); //$NON-NLS-1$
		supportedExtensions.add("CPP"); //$NON-NLS-1$
		supportedExtensions.add("c++"); //$NON-NLS-1$
		supportedExtensions.add("C++"); //$NON-NLS-1$
		supportedExtensions.add("cp"); //$NON-NLS-1$
		supportedExtensions.add("CP"); //$NON-NLS-1$
		supportedExtensions.add("cxx"); //$NON-NLS-1$
		supportedExtensions.add("CXX"); //$NON-NLS-1$
		supportedExtensions.add("h"); //$NON-NLS-1$
		supportedExtensions.add("H"); //$NON-NLS-1$
		supportedExtensions.add("hh"); //$NON-NLS-1$
		supportedExtensions.add("HH"); //$NON-NLS-1$
		supportedExtensions.add("hpp"); //$NON-NLS-1$
		supportedExtensions.add("HPP"); //$NON-NLS-1$
	}
	
	/**
	 * Display the command line usage message.
	 */
	private void displayHelp() {
		System.out.println(Messages.bind(Messages.CommandLineUsage));
	}

	private void displayHelp(String message) {
		System.err.println(message);
		System.out.println();
		displayHelp();
	}

	/**
	 * Recursively format the C/C++ source code that is contained in the
	 * directory rooted at dir.
	 */
	private void formatDirTree(File dir, CodeFormatter codeFormatter) {

		File[] files = dir.listFiles();
		if (files == null)
			return;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				formatDirTree(file, codeFormatter);
			//} else if (Util.isJavaLikeFileName(file.getPath())) {
			} else if (isCppLikeFileName(file.getPath())) {
				formatFile(file, codeFormatter);
			}
		}
	}

	/**
	 * Returns the given input stream's contents as a character array.
	 * If a length is specified (i.e. if length != -1), this represents the number of bytes in the stream.
	 * Note this doesn't close the stream.
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding)
			throws IOException {
		BufferedReader reader = null;
		try {
			reader = encoding == null
						? new BufferedReader(new InputStreamReader(stream))
						: new BufferedReader(new InputStreamReader(stream, encoding));
		} catch (UnsupportedEncodingException e) {
			// encoding is not supported
			reader =  new BufferedReader(new InputStreamReader(stream));
		}
		char[] contents;
		int totalRead = 0;
		if (length == -1) {
			contents = CharOperation.NO_CHAR;
		} else {
			// length is a good guess when the encoding produces less or the same amount of characters than the file length
			contents = new char[length]; // best guess
		}

		while (true) {
			int amountRequested;
			if (totalRead < length) {
				// until known length is met, reuse same array sized eagerly
				amountRequested = length - totalRead;
			} else {
				// reading beyond known length
				int current = reader.read();
				if (current < 0) break;

				amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);  // read at least 8K

				// resize contents if needed
				if (totalRead + 1 + amountRequested > contents.length)
					System.arraycopy(contents, 	0, 	contents = new char[totalRead + 1 + amountRequested], 0, totalRead);

				// add current character
				contents[totalRead++] = (char) current; // coming from totalRead==length
			}
			// read as many chars as possible
			int amountRead = reader.read(contents, totalRead, amountRequested);
			if (amountRead < 0) break;
			totalRead += amountRead;
		}

		// Do not keep first character for UTF-8 BOM encoding
		int start = 0;
		if (totalRead > 0 && UTF_8.equals(encoding)) {
			if (contents[0] == 0xFEFF) { // if BOM char then skip
				totalRead--;
				start = 1;
			}
		}

		// resize contents if necessary
		if (totalRead < contents.length)
			System.arraycopy(contents, start, contents = new char[totalRead], 	0, 	totalRead);

		return contents;
	}

	
	/**
	 * Returns the contents of the given file as a char array.
	 * When encoding is null, then the platform default one is used
	 * @throws IOException if a problem occured reading the file.
	 */
	public static char[] getFileCharContent(File file, String encoding) throws IOException {
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			return getInputStreamAsCharArray(stream, (int) file.length(), encoding);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Format the given C/C++ source file.
	 */
	private void formatFile(File file, CodeFormatter codeFormatter) {
		IDocument doc = new Document();
		try {
			// read the file
			if (this.verbose) {
				System.out.println(Messages.bind(Messages.CommandLineFormatting, file.getAbsolutePath()));
			}
			// String contents = new String(org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(file, null));
			String contents = new String(/*org.eclipse.jdt.internal.compiler.util.Util.*/getFileCharContent(file, null));
			// format the file (the meat and potatoes)
			doc.set(contents);
			TextEdit edit = codeFormatter.format(CodeFormatter.K_TRANSLATION_UNIT, contents, 0, contents.length(), 0, null);
			if (edit != null) {
				edit.apply(doc);
			} else {
				System.err.println(Messages.bind(Messages.FormatProblem, file.getAbsolutePath()));
				return;
			}

			// write the file
			final BufferedWriter out = new BufferedWriter(new FileWriter(file));
			try {
				out.write(doc.get());
				out.flush();
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		} catch (IOException e) {
			String errorMessage = Messages.bind(Messages.CaughtException, "IOException", e.getLocalizedMessage()); //$NON-NLS-1$
			//Util.log(e, errorMessage);
			System.err.println(e.getMessage());
			System.err.println(errorMessage);
			System.err.println(Messages.bind(Messages.ExceptionSkip ,errorMessage));
		} catch (BadLocationException e) {
			String errorMessage = Messages.bind(Messages.CaughtException, "BadLocationException", e.getLocalizedMessage()); //$NON-NLS-1$
			//Util.log(e, errorMessage);
			System.err.println(e.getMessage());
			System.err.println(errorMessage);
			System.err.println(Messages.bind(Messages.ExceptionSkip ,errorMessage));
		}
	}
	
	
	
	private File[] processCommandLine(String[] argsArray) {

		ArrayList args = new ArrayList();
		for (int i = 0, max = argsArray.length; i < max; i++) {
			args.add(argsArray[i]);
		}
		int index = 0;
		final int argCount = argsArray.length;

		final int DEFAULT_MODE = 0;
		final int CONFIG_MODE = 1;

		int mode = DEFAULT_MODE;
		final int INITIAL_SIZE = 1;
		int fileCounter = 0;

		File[] filesToFormat = new File[INITIAL_SIZE];

		loop: while (index < argCount) {
			String currentArg = argsArray[index++];

			switch(mode) {
				case DEFAULT_MODE :
					if (PDE_LAUNCH.equals(currentArg)) {
						continue loop;
					}
					if (ARG_HELP.equals(currentArg)) {
						displayHelp();
						return null;
					}
					if (ARG_VERBOSE.equals(currentArg)) {
						this.verbose = true;
						continue loop;
					}
					if (ARG_QUIET.equals(currentArg)) {
						this.quiet = true;
						continue loop;
					}
					if (ARG_CONFIG.equals(currentArg)) {
						mode = CONFIG_MODE;
						continue loop;
					}
					// the current arg should be a file or a directory name
					File file = new File(currentArg);
					if (file.exists()) {
						if (filesToFormat.length == fileCounter) {
							System.arraycopy(filesToFormat, 0, (filesToFormat = new File[fileCounter * 2]), 0, fileCounter);
						}
						filesToFormat[fileCounter++] = file;
					} else {
						String canonicalPath;
						try {
							canonicalPath = file.getCanonicalPath();
						} catch(IOException e2) {
							canonicalPath = file.getAbsolutePath();
						}
						String errorMsg = file.isAbsolute()?
										  Messages.bind(Messages.CommandLineErrorFile, canonicalPath):
										  Messages.bind(Messages.CommandLineErrorFileTryFullPath, canonicalPath);
						displayHelp(errorMsg);
						return null;
					}
					break;
				case CONFIG_MODE :
					this.configName = currentArg;
					this.options = readConfig(currentArg);
					if (this.options == null) {
						displayHelp(Messages.bind(Messages.CommandLineErrorConfig, currentArg));
						return null;
					}
					mode = DEFAULT_MODE;
					continue loop;
			}
		}

		if (mode == CONFIG_MODE || this.options == null) {
			displayHelp(Messages.bind(Messages.CommandLineErrorNoConfigFile));
			return null;
		}
		if (this.quiet && this.verbose) {
			displayHelp(
				Messages.bind(
					Messages.CommandLineErrorQuietVerbose,
					new String[] { ARG_QUIET, ARG_VERBOSE }
				));
			return null;
		}
		if (fileCounter == 0) {
			displayHelp(Messages.bind(Messages.CommandLineErrorFileDir));
			return null;
		}
		if (filesToFormat.length != fileCounter) {
			System.arraycopy(filesToFormat, 0, (filesToFormat = new File[fileCounter]), 0, fileCounter);
		}
		return filesToFormat;
	}

	/**
	 * Return a Properties file representing the options that are in the
	 * specified configuration file.
	 */
	private Properties readConfig(String filename) {
		BufferedInputStream stream = null;
		File configFile = new File(filename);
		try {
			stream = new BufferedInputStream(new FileInputStream(configFile));
			final Properties formatterOptions = new Properties();
			formatterOptions.load(stream);
			return formatterOptions;
		} catch (IOException e) {
			String canonicalPath = null;
			try {
				canonicalPath = configFile.getCanonicalPath();
			} catch(IOException e2) {
				canonicalPath = configFile.getAbsolutePath();
			}
			String errorMessage;
			if (!configFile.exists() && !configFile.isAbsolute()) {
				errorMessage = Messages.bind(Messages.ConfigFileNotFoundErrorTryFullPath, new Object[] {
					canonicalPath,
					System.getProperty("user.dir") //$NON-NLS-1$
				});

			} else {
				errorMessage = Messages.bind(Messages.ConfigFileReadingError, canonicalPath);
			}
			//Util.log(e, errorMessage);
			System.err.println(e.getMessage());
			System.err.println(errorMessage);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns true if the given name ends with one of the known C/C++ like extension.
	 * (implementation is not creating extra strings)
	 */
	public final static boolean isCppLikeFileName(String name) {
		if (name == null)
			return false;
		// return indexOfJavaLikeExtension(name) != -1;
		String[] lExt = name.split("\\."); //$NON-NLS-1$
		if (lExt.length < 2) {
			return false;
		}
		return supportedExtensions.contains(lExt[lExt.length - 1]);
	}
	
	/**
	 * Runs the C/C++ code formatter application
	 */
	// public Object start(IApplicationContext context) throws Exception {
	public Object start(String[] args) throws Exception {
		//File[] filesToFormat = processCommandLine((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
		File[] filesToFormat = processCommandLine(args);

		if (filesToFormat == null) {
			return IApplication.EXIT_OK;
		}

		if (!this.quiet) {
			if (this.configName != null) {
				System.out.println(Messages.bind(Messages.CommandLineConfigFile, this.configName));
			}
			System.out.println(Messages.bind(Messages.CommandLineStart));
		}

		//final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(this.options);
		final CCodeFormatter codeFormatter = new CCodeFormatter(this.options);
		// format the list of files and/or directories
		for (int i = 0, max = filesToFormat.length; i < max; i++) {
			final File file = filesToFormat[i];
			if (file.isDirectory()) {
				formatDirTree(file, codeFormatter);
			// } else if (Util.isJavaLikeFileName(file.getPath())) {
			} else if (isCppLikeFileName(file.getPath())) {
				formatFile(file, codeFormatter);
			}
		}
		if (!this.quiet) {
			System.out.println(Messages.bind(Messages.CommandLineDone));
		}

		return IApplication.EXIT_OK;
	}
	public void stop() {
		// do nothing
	}
	
	
	public static void main(String[] args) {
		CCodeFormatterApp app = new CCodeFormatterApp();
		try {
			app.start(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
