package com.google.drive;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class GoogleDriveAPI {
	  /**
	   * Be sure to specify the name of your application. If the application name is {@code null} or
	   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
	   */
	  private static final String APPLICATION_NAME = "";

	  private static final String UPLOAD_FILE_PATH = "D:\\Ming\\";
	  private static final String DIR_FOR_DOWNLOADS = "D:\\Ming\\";
//	  private static final java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);

	  /** Directory to store user credentials. */
	  private static final java.io.File DATA_STORE_DIR =
	      new java.io.File(System.getProperty("user.home"), ".store/drive_sample");

	  /**
	   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
	   * globally shared instance across your application.
	   */
	  private static FileDataStoreFactory dataStoreFactory;

	  /** Global instance of the HTTP transport. */
	  private static HttpTransport httpTransport;

	  /** Global instance of the JSON factory. */
	  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	  /** Global Drive API client. */
	  private static Drive drive;

	  /** Authorizes the installed application to access user's protected data. */
	  private static Credential authorize() throws Exception {
		// load client secrets
		String filePath = "client_secret.json";
		InputStreamReader in = new InputStreamReader(new FileInputStream(filePath), "UTF-8");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, in);
	    if (clientSecrets.getDetails().getClientId().startsWith("Enter")
	        || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
	      System.out.println(
	          "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive "
	          + "into drive-cmdline-sample/src/main/resources/client_secrets.json");
	      System.exit(1);
	    }
	    // set up authorization code flow
	    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	        httpTransport, JSON_FACTORY, clientSecrets,
	        Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
	        .build();
	    // authorize
	    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	  }
	  
	  public static void main(String[] args) {
	    Preconditions.checkArgument(
	        !UPLOAD_FILE_PATH.startsWith("Enter ") && !DIR_FOR_DOWNLOADS.startsWith("Enter "),
	        "Please enter the upload file path and download directory in %s", GoogleDriveAPI.class);

	    try {
	      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
	      // authorization
	      Credential credential = authorize();
	      // set up the global Drive instance
	      drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(
	          APPLICATION_NAME).build();

	      // run commands

//	      View.header1("Start/ing Resumable Media Upload");
//	      File uploadedFile = uploadFile(false);
	      
	      View.header1("Searching folder");
	      searchFolder("");
	      
//	      View.header1("Updating Uploaded File Name");
//	      File updatedFile = updateFileWithTestSuffix(uploadedFile.getId());
//
//	      View.header1("Starting Resumable Media Download");
//	      downloadFile(false, updatedFile);
//
//	      View.header1("Starting Simple Media Upload");
//	      uploadedFile = uploadFile(true);
//
//	      View.header1("Starting Simple Media Download");
//	      downloadFile(true, uploadedFile);
//
//	      View.header1("Success!");
	      return;
	    } catch (IOException e) {
	      System.err.println(e.getMessage());
	    } catch (Throwable t) {
	      t.printStackTrace();
	    }
	    System.exit(1);
	  }

	  private static String createFolder(String folderName) throws IOException {
		  File fileMetadata = new File();
		  fileMetadata.setName(folderName);
		  fileMetadata.setMimeType("application/vnd.google-apps.folder");

		  File file = drive.files().create(fileMetadata)
		      .setFields("id")
		      .execute();
		  System.out.println("Folder ID: " + file.getId());
		  return file.getId();
	  }
	  
	private static boolean searchFolder(String folderName) {
		boolean foundFlag = false;
		String pageToken = "";
		try {
			do {
				FileList result = drive.files().list()
						.setQ("mimeType = 'application/vnd.google-apps.folder'")
						.setSpaces("drive")
						.setFields("nextPageToken, files(id, name)")
						.setPageToken(pageToken).execute();
				for (File file : result.getFiles()) {
					System.out.printf("Found file: %s (%s)\n", file.getName(),
							file.getId());
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return foundFlag;
	}
	  
	  /**
	   * Uploads a file using either resumable or direct media upload.
	   * @param useDirectUpload
	   * @return
	   * @throws IOException
	   */
	  private static File uploadFile(boolean useDirectUpload) throws IOException {
		// Create new folder
		String folderName = "Test";
		String folderId =  createFolder(folderName);		
		  
	    File fileMetadata = new File();
//	    fileMetadata.setTitle(UPLOAD_FILE.getName());
//	    fileMetadata.setName(UPLOAD_FILE.getName());
	    String fileName = "1011_Pens-Logo-1400x864.jpg";
	    fileMetadata.setName(fileName);
	    // Set which folder that the photo is going to be saved in 
	    fileMetadata.setParents(Collections.singletonList(folderId));

	    java.io.File filePath = new java.io.File(UPLOAD_FILE_PATH+fileName);
	    
	    FileContent mediaContent = new FileContent("image/jpeg", filePath);
	    File file = drive.files().create(fileMetadata, mediaContent).setFields("id, parents").execute();
	    
	    return file;
	  }

	  /**
	   * Updates the name of the uploaded file to have a "drivetest-" prefix.
	   * @param id
	   * @return
	   * @throws IOException
	   */
	  private static File updateFileWithTestSuffix(String id) throws IOException {
	    File fileMetadata = new File();
//	    fileMetadata.setTitle("drivetest-" + UPLOAD_FILE.getName());
//	    fileMetadata.setName("drivetest-" + UPLOAD_FILE.getName());
	    String fileName = "1011_Pens-Logo-1400x864.jpg";
	    java.io.File filePath = new java.io.File(UPLOAD_FILE_PATH+fileName);
	    fileMetadata.setName("drivetest-" + filePath.getName());
	    
	    Drive.Files.Update update = drive.files().update(id, fileMetadata);
	    return update.execute();
	  }

	  /** 
	   * Downloads a file using either resumable or direct media download.
	   * @param useDirectDownload
	   * @param uploadedFile
	   * @throws IOException
	   */
	  private static void downloadFile(boolean useDirectDownload, File uploadedFile)
	      throws IOException {
	    // create parent directory (if necessary)
	    java.io.File parentDir = new java.io.File(DIR_FOR_DOWNLOADS);
	    if (!parentDir.exists() && !parentDir.mkdirs()) {
	      throw new IOException("Unable to create parent directory");
	    }
//	    OutputStream out = new FileOutputStream(new java.io.File(parentDir, uploadedFile.getTitle()));
	    OutputStream out = new FileOutputStream(new java.io.File(parentDir, uploadedFile.getName()));

	    MediaHttpDownloader downloader =
	        new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());
	    downloader.setDirectDownloadEnabled(useDirectDownload);
	    downloader.setProgressListener(new FileDownloadProgressListener());
//	    downloader.download(new GenericUrl(uploadedFile.getDownloadUrl()), out);
	    downloader.download(new GenericUrl(uploadedFile.getName()), out);
	  }
}
