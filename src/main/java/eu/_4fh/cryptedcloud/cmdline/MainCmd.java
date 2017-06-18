package eu._4fh.cryptedcloud.cmdline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Supplier;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.KeyStore;
import eu._4fh.cryptedcloud.util.Util;

public class MainCmd {
	private enum ROOT_COMMANDS {
		ENCRYPT("encrypt", "Encrypt a file", () -> createEncrypt()), DECRYPT("decrypt", "Decrypt a file",
				() -> createDecrypt()), CONFIG("config", "Change configuration", () -> createConfig()), CREATE_KEY(
						"createkey", "Create a new private key", () -> createCreateKey()), EXPORT_KEY("exportkey",
								"Export a key", () -> createExportKey()), ADD_PRIV_KEY("addprivkey",
										"Add a private key", () -> createAddPrivKey()), ADD_PUB_KEY("addpubkey",
												"Add a public key", () -> createAddPubKey());
		private final String rootCommandName;
		private final String rootCommandDesc;
		private final Supplier<Options> createMethod;

		private ROOT_COMMANDS(final String command, final String desc, Supplier<Options> createMethod) {
			this.rootCommandName = command;
			this.rootCommandDesc = desc;
			this.createMethod = createMethod;
		}

		public Option createRootCommand() {
			return Option.builder(rootCommandName).desc(rootCommandDesc).required().build();
		}

		public Options createOptions() {
			Options ret = createMethod.get();
			ret.addOption(createRootCommand());
			return ret;
		}

		static private Options createAddPubKey() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("file").desc("Public Key file").hasArg().required()
					.type(String.class).build());
			ret.addOption(
					Option.builder().longOpt("password").desc("Passwort for key").hasArg().type(String.class).build());
			return ret;
		}

		static private Options createAddPrivKey() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("file").desc("Private Key file").hasArg().required()
					.type(String.class).build());
			ret.addOption(
					Option.builder().longOpt("password").desc("Passwort for key").hasArg().type(String.class).build());
			return ret;
		}

		static private Options createExportKey() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("name").desc("Name of key to export").hasArg().required()
					.type(String.class).build());
			ret.addOption(Option.builder().longOpt("file").desc("Name for target file").hasArg().required()
					.type(String.class).build());
			ret.addOption(Option.builder().longOpt("password").desc("Password for exported key").hasArg()
					.type(String.class).build());
			OptionGroup group = new OptionGroup();
			group.setRequired(true);
			group.addOption(Option.builder().longOpt("privkey").desc("Export private key").build());
			group.addOption(Option.builder().longOpt("pubkey").desc("Export public key").build());
			ret.addOptionGroup(group);
			return ret;
		}

		static private Options createCreateKey() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("name").desc("Name for new key").hasArg().required()
					.type(String.class).build());
			return ret;
		}

		static private Options createConfig() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("fileChunkSize").desc("Set file chunk size").hasArg()
					.type(Long.class).build());
			ret.addOption(Option.builder().longOpt("configDir").desc("Set directory for config values").hasArg()
					.type(String.class).build());
			ret.addOption(Option.builder().longOpt("tempDir").desc("Set directory for temporary files").hasArg()
					.type(String.class).build());
			return ret;
		}

		static private Options createDecrypt() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("file").desc("File to decrypt").hasArg().required()
					.type(String.class).build());
			return ret;
		}

		static private Options createEncrypt() {
			Options ret = new Options();
			ret.addOption(Option.builder().longOpt("file").desc("File to encrypt").hasArg().required()
					.type(String.class).build());
			return ret;
		}
	}

	final private String args[];
	final private String jarFile;

	public MainCmd(final String jarFile, final String[] args) {
		this.jarFile = jarFile;
		this.args = args;
	}

	public void run() throws ParseException {
		Options tmpOptions = new Options();
		tmpOptions.addOption(Option.builder("h").longOpt("help").build());
		CommandLine cmd = new DefaultParser().parse(tmpOptions, args, true);
		if (cmd.hasOption("h")) {
			printHelp(new PrintWriter(System.out));
			return;
		}

		ROOT_COMMANDS rootCommand = getSelectedRootCommand();
		cmd = new DefaultParser().parse(rootCommand.createOptions(), args, false);
		execOption(rootCommand, cmd);
	}

	private void printHelp(final PrintWriter writer) {
		HelpFormatter formatter = new HelpFormatter();
		for (ROOT_COMMANDS rootCommand : ROOT_COMMANDS.values()) {
			formatter.printHelp(writer, 120, jarFile, "", rootCommand.createOptions(), 2, 4, "", true);
		}
		writer.flush();
	}

	private OptionGroup buildRootOptionGroup() {
		OptionGroup ret = new OptionGroup();
		ret.setRequired(true);
		for (ROOT_COMMANDS rootCommand : ROOT_COMMANDS.values()) {
			ret.addOption(rootCommand.createRootCommand());
		}
		return ret;
	}

	private ROOT_COMMANDS getSelectedRootCommand() throws ParseException {
		Options tmpOptions = new Options();
		OptionGroup rootOption = buildRootOptionGroup();
		tmpOptions.addOptionGroup(rootOption);
		CommandLine cmd = new DefaultParser().parse(tmpOptions, args, true);
		String selectedCommand = rootOption.getSelected();

		for (ROOT_COMMANDS rootCommand : ROOT_COMMANDS.values()) {
			if (cmd.hasOption(rootCommand.createRootCommand().getOpt())) {
				return rootCommand;
			}
		}
		throw new IllegalStateException("Cant find command for selectedCommand=" + selectedCommand);
	}

	private boolean checkSourceAndDestFile(final File srcFile, final File dstFile) {
		if (!srcFile.exists() || !srcFile.canRead()) {
			System.out.println("Cant read input file " + srcFile.getAbsolutePath());
			return false;
		}
		if (dstFile.exists() && !dstFile.canWrite()) {
			System.out.println("Cant write output file " + dstFile.getAbsolutePath());
			return false;
		}
		if (dstFile.exists()) {
			System.out.println("Output file already exists: " + dstFile.getAbsolutePath());
			return false;
		}
		return true;
	}

	private void execOption(ROOT_COMMANDS rootCommand, CommandLine cmd) throws ParseException {
		if (ROOT_COMMANDS.ENCRYPT.equals(rootCommand)) {
			handleEncryptCommand(cmd);
		} else if (ROOT_COMMANDS.DECRYPT.equals(rootCommand)) {
			handleDecryptCommand(cmd);
		} else if (ROOT_COMMANDS.CONFIG.equals(rootCommand)) {
			handleConfigCommand(cmd);
		} else if (ROOT_COMMANDS.CREATE_KEY.equals(rootCommand)) {
			handleCreateKeyCommand(cmd);
		} else if (ROOT_COMMANDS.ADD_PRIV_KEY.equals(rootCommand)) {
			handleAddPrivKeyCommand(cmd);
		} else if (ROOT_COMMANDS.ADD_PUB_KEY.equals(rootCommand)) {
			handleAddPubKeyCommand(cmd);
		} else if (ROOT_COMMANDS.EXPORT_KEY.equals(rootCommand)) {
			handleExportKeyCommand(cmd);
		} else {
			throw new IllegalArgumentException(
					"Missing method to handle RootCommand " + rootCommand.createRootCommand().toString());
		}
	}

	private void handleEncryptCommand(CommandLine cmd) throws ParseException {
		final File srcFile = new File((String) cmd.getParsedOptionValue("file"));
		final File dstFile = new File(srcFile.getAbsolutePath() + ".enc");

		if (!checkSourceAndDestFile(srcFile, dstFile)) {
			return;
		}

		Util.FileEncryptionCallback callback = new Util.FileEncryptionCallback() {
			@Override
			public void finished() {
				System.out.println(
						"Encrypted \"" + srcFile.getAbsolutePath() + "\" to \"" + dstFile.getAbsolutePath() + "\".");
			}

			@Override
			public void error(Throwable t) {
				System.out.println(("Error while encrypting file \"" + srcFile.getAbsolutePath() + "\":"));
				t.printStackTrace(System.out);
			}
		};

		Util.createEncryptFileThread(srcFile, dstFile, callback).start();
	}

	private void handleDecryptCommand(CommandLine cmd) throws ParseException {
		File srcFile = new File((String) cmd.getParsedOptionValue("file"));
		String dstFilePath = srcFile.getAbsolutePath();
		if (dstFilePath.endsWith(".enc")) {
			dstFilePath = dstFilePath.substring(0, dstFilePath.length() - 4);
		} else {
			dstFilePath = dstFilePath + ".dec";
		}
		final @NonNull File dstFile = new File(dstFilePath);

		if (!checkSourceAndDestFile(srcFile, dstFile)) {
			return;
		}

		Util.FileEncryptionCallback callback = new Util.FileEncryptionCallback() {
			@Override
			public void finished() {
				System.out.println("Encrytped file \"" + srcFile.getAbsolutePath() + "\" to \""
						+ dstFile.getAbsolutePath() + "\".");
			}

			@Override
			public void error(Throwable t) {
				System.out.println("Error encrypting file \"" + srcFile.getAbsolutePath() + "\":");
				t.printStackTrace(System.out);
			}
		};
		Thread t = Util.createDecryptFileThread(srcFile, dstFile, callback);
		t.start();
		while (t.isAlive()) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	private void handleConfigCommand(CommandLine cmd) {
		synchronized (Config.WritableConfig.class) {
			try {
				Config.WritableConfig config = Config.getInstance().getWritableConfig();
				if (cmd.hasOption("fileChunkSize")) {
					config.setFileChunkSize((int) cmd.getParsedOptionValue("fileChunkSize"));
				}
				if (cmd.hasOption("configDir")) {
					config.setConfigDir(new File((String) cmd.getParsedOptionValue("configDir")));
				}
				if (cmd.hasOption("tempDir")) {
					config.setTempDir(new File((String) cmd.getParsedOptionValue("tempDir")));
				}
				Config.writeAndReloadConfig(config);
			} catch (IOException | ParseException e) {
				System.out.println("Cant write config:");
				e.printStackTrace(System.out);
			}
		}
	}

	private void handleCreateKeyCommand(CommandLine cmd) throws ParseException {
		final String keyName = (String) cmd.getParsedOptionValue("name");
		if (keyName != null && !keyName.trim().equals("")) {
			KeyStore.getInstance().createKey(keyName);
			saveKeyStore();
		}
	}

	private void handleAddPrivKeyCommand(CommandLine cmd) throws ParseException {
		final @NonNull File file = new File((String) cmd.getParsedOptionValue("file"));

		String password = null;
		if (cmd.hasOption("password")) {
			password = (String) cmd.getParsedOptionValue("password");
			if (password != null && password.trim().equals("")) {
				password = null;
			}
		}

		try {
			KeyStore.getInstance().importKey(true, password, file);
		} catch (IOException e) {
			System.out.println("Cant add private key: ");
			e.printStackTrace(System.out);
		}
		saveKeyStore();
	}

	private void handleAddPubKeyCommand(CommandLine cmd) throws ParseException {
		final @NonNull File file = new File((String) cmd.getParsedOptionValue("file"));

		String password = null;
		if (cmd.hasOption("password")) {
			password = (String) cmd.getParsedOptionValue("password");
			if (password != null && password.trim().equals("")) {
				password = null;
			}
		}

		try {
			KeyStore.getInstance().importKey(false, password, file);
		} catch (IOException e) {
			System.out.println("Cant add public key: ");
			e.printStackTrace(System.out);
		}
		saveKeyStore();
	}

	private void handleExportKeyCommand(CommandLine cmd) throws ParseException {
		final @NonNull String keyName = Util.checkNonNull((String) cmd.getParsedOptionValue("name"));
		final File targetFile = new File((String) cmd.getParsedOptionValue("file"));
		if (targetFile.exists()) {
			System.out.println("Target file already exists: " + targetFile.getAbsolutePath());
			return;
		}
		String password = null;
		if (cmd.hasOption("password")) {
			password = (String) cmd.getParsedOptionValue("password");
			if (password != null && password.trim().equals("")) {
				password = null;
			}
		}
		final boolean privateKey = cmd.hasOption("privkey");

		try {
			KeyStore.getInstance().exportKey(privateKey, keyName, password, targetFile);
		} catch (IOException e) {
			System.out.println("Cant export key: ");
			e.printStackTrace(System.out);
		}
	}

	private void saveKeyStore() {
		try {
			KeyStore.getInstance().writeToFile();
		} catch (IOException e) {
			System.out.println("Cant write keys to file:");
			e.printStackTrace(System.out);
		}
	}
}
