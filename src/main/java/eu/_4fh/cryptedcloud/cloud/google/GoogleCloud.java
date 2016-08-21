package eu._4fh.cryptedcloud.cloud.google;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.extensions.java6.auth.oauth2.GooglePromptReceiver;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;

import eu._4fh.cryptedcloud.cloud.AbstractCloudService;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;
import eu._4fh.cryptedcloud.util.Util.Pair;

public class GoogleCloud extends AbstractCloudService {
	private static final Logger log = Util.getLogger();
	private static final String CLIENT_ID_FILE = "/resource_files/google_client_id.json";
	/**
	 * Be sure to specify the name of your application. If the application name
	 * is {@code null} or blank, the application will log a warning. Suggested
	 * format is "MyCompany-ProductName/1.0". The text "gzip" should appear in
	 * the application-name. That name is used as user-agent header and
	 * according to https://developers.google.com/drive/v3/web/performance gzip
	 * is only activated when the text "gzip" appears in the user-agent
	 */
	private static final String APPLICATION_NAME = "4fh.eu-CrytpedCloud/1.0 (gzip)";
	static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	static final String FILE_FIELDS = "id,name,mimeType,modifiedTime,modifiedByMeTime,parents";

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to
	 * make it a single globally shared instance across your application.
	 */
	private final FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the HTTP transport. */
	private final HttpTransport HTTP_TRANSPORT;

	/** Global instance of the JSON factory. */
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global Drive API client. */
	private final Drive DRIVE;

	private GoogleFolder ROOT_FOLDER;

	private Util.Pair<Long, Long> usageAndLimit;
	private final String userName;

	/**
	 * Authorizes the installed application to access user's protected data.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private Credential authorize() throws FileNotFoundException, IOException {
		// load client secrets
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
				new InputStreamReader(getClass().getResourceAsStream(CLIENT_ID_FILE)));
		// set up authorization code flow
		Set<String> scopes = new HashSet<String>();
		scopes.add(DriveScopes.DRIVE_READONLY);
		scopes.add(DriveScopes.DRIVE_METADATA_READONLY);
		scopes.add(DriveScopes.DRIVE_FILE);
		scopes.add(DriveScopes.DRIVE);
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, Collections.unmodifiableSet(scopes)).setDataStoreFactory(DATA_STORE_FACTORY).build();
		// authorize
		VerificationCodeReceiver receiver = new GooglePromptReceiver();
		if (!java.awt.GraphicsEnvironment.isHeadless()) {
			receiver = new VerificationCodeReceiver() {
				@Override
				public String waitForCode() throws IOException {
					String result = (String) JOptionPane.showInputDialog(null,
							"Your browser should open a webpage asking for permissions. Please copy the code here after you granted the permissions.",
							"Google Authorization", JOptionPane.QUESTION_MESSAGE);
					if (result == null) {
						log.warning("Canceled google authorization dialog.");
						return "";
					}
					return result;
				}

				@Override
				public void stop() throws IOException {
					// Can't do anything, don't have to do anything
				}

				@Override
				public String getRedirectUri() throws IOException {
					return GoogleOAuthConstants.OOB_REDIRECT_URI;
				}
			};
		}
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public GoogleCloud() throws GeneralSecurityException, IOException {
		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		DATA_STORE_FACTORY = new FileDataStoreFactory(Config.getInstance().getGoogleConfig().getDataStoreDir());
		log.finer(() -> "Trying to authorize...");
		Credential credential = authorize();
		log.finer(() -> "Authorization completed");
		DRIVE = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
				.build();

		log.finer(() -> "DRIVE initialized, fetch files");
		Map<String, com.google.api.services.drive.model.File> files = new HashMap<String, com.google.api.services.drive.model.File>();
		String pageToken = null;
		do {
			FileList result = DRIVE.files().list().setSpaces("drive").setCorpus("user")
					.setQ("\'" + Config.getInstance().getGoogleConfig().getUserEMail()
							+ "' in owners and trashed!=true")
					.setFields("files(" + FILE_FIELDS + "),nextPageToken").setPageToken(pageToken).execute();
			for (com.google.api.services.drive.model.File file : result.getFiles()) {
				log.finest(() -> "Found: " + file.getName() + "(" + file.getId() + ") " + file.getMimeType()
						+ " parent " + file.getParents().get(0));
				files.put(file.getId(), file);
			}
			pageToken = result.getNextPageToken();
		} while (pageToken != null);

		for (Map.Entry<String, com.google.api.services.drive.model.File> fileEntry : files.entrySet()) {
			com.google.api.services.drive.model.File file = fileEntry.getValue();
			if ((file.getParents() == null || file.getParents().isEmpty()
					|| !files.containsKey(file.getParents().get(0)))
					&& file.getName().equals(Config.getInstance().getGoogleConfig().getCrytpedCloudRootFolderName())
					&& file.getMimeType().equals(FOLDER_MIME_TYPE)) {
				log.finer(() -> "Found root folder: " + file.getName() + "(" + file.getId() + ")");
				ROOT_FOLDER = new GoogleFolder(this, file);
				break;
			}
		}

		if (ROOT_FOLDER == null) {
			throw new RuntimeException("Can't find root-folder with name"
					+ Config.getInstance().getGoogleConfig().getCrytpedCloudRootFolderName());
		}

		AbstractCloudService.TreeBuilderHelper<GoogleFolder, GoogleFile> treeBuilderHelper = new AbstractCloudService.TreeBuilderHelper<GoogleFolder, GoogleFile>() {

			@Override
			public boolean isFolder(Object fileOrFolder) {
				return ((com.google.api.services.drive.model.File) fileOrFolder).getMimeType()
						.equalsIgnoreCase(FOLDER_MIME_TYPE);
			}

			@Override
			public GoogleFolder constructFolder(Object folder, GoogleFolder parent) {
				return parent.addGoogleFolder(((com.google.api.services.drive.model.File) folder));
			}

			@Override
			public GoogleFile constructFile(Object file, GoogleFolder parent) {
				return parent.addGoogleFile(((com.google.api.services.drive.model.File) file));
			}

			@Override
			public String getFolderId(GoogleFolder folder) {
				return folder.getFile().getId();
			}

			@Override
			public String getParentId(Object fileOrFolder) {
				return ((com.google.api.services.drive.model.File) fileOrFolder).getParents().get(0);
			}

		};
		constructTree(files, ROOT_FOLDER, treeBuilderHelper);
		log.finest(() -> "File tree: " + ROOT_FOLDER.toStringRecursive());

		userName = DRIVE.about().get().setFields("kind,user").execute().getUser().getDisplayName();
		usageAndLimit = new Util.Pair<Long, Long>(0L, 0L);
		usageAndLimit = getUsageAndLimit(true);
		log.finer(() -> "Current usage: " + usageAndLimit.toString());
	}

	@Nonnull
	Drive getDrive() {
		return DRIVE;
	}

	@Nonnull
	JsonFactory getJsonFactory() {
		return JSON_FACTORY;
	}

	@Override
	public @Nonnull GoogleFolder getRootFolder() {
		return ROOT_FOLDER;
	}

	@Override
	public @Nonnull Pair<Long, Long> getUsageAndLimit(boolean refresh) throws IOException {
		if (refresh) {
			About about = DRIVE.about().get()
					.setFields("appInstalled,kind,maxImportSizes,maxUploadSize,storageQuota,user").execute();

			Long usage = about.getStorageQuota().getUsage();
			if (usage == null || usage < 0) {
				usage = 0L;
			}

			Long limit = about.getStorageQuota().getLimit();
			if (limit == null || limit < 0) {
				limit = -1L;
			}

			usageAndLimit = new Util.Pair<Long, Long>(usage, limit);
		}
		return usageAndLimit;
	}

	@Override
	public @Nonnull String getUserName() {
		return userName;
	}
}
