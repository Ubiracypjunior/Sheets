package gradleproject1;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class App {
    
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
    * Global instance of the scopes required by this quickstart.
    * If modifying these scopes, delete your previously saved tokens/ folder.
    */
    private static final List<String> SCOPES =
        Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
    * Creates an authorized Credential object.
    *
    * @param HTTP_TRANSPORT The network HTTP Transport.
    * @return An authorized Credential object.
    * @throws IOException If the credentials.json file cannot be found.
    */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
        throws IOException {
        // Load client secrets.
        InputStream in = App.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
        throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     *
     * @return Returns a Sheets class service with the autorization necessary to modify a Google's spreadsheet.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static Sheets service() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     *
     * @param spreadsheetId Is the ID located in the spreadsheet's URL.
     * @param range Indicates where the data is located in the spreadsheet.
     * @return Returns a List of where each list contains an row from the spreadsheet
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static List<List<Object>> getSpreadSheetData(String spreadsheetId, String range) throws IOException, GeneralSecurityException {
        //Create a List<List<Object>> with the spreadsheet's calculated data.
        ValueRange response = service().spreadsheets().values()
            .get(spreadsheetId, range)
            .execute();
        
        List<Object> abscence = Arrays.asList("Reprovado por Falta", "0");
        List<Object> grades = Arrays.asList("Reprovado por Nota", "0");
        List<Object> approved = Arrays.asList("Aprovado", "0");
        
        List<List<Object>> result = new ArrayList<>();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
        System.out.println("Nenhum dado encontrado.");
        } else {
        for (int r1 = 0; r1 < values.size(); r1++) {
            int studentAbscence = Integer.parseInt(values.get(r1).get(2).toString());
            int semesterLessons = 60;
            
            int p1 = Integer.parseInt(values.get(r1).get(3).toString());
            int p2 = Integer.parseInt(values.get(r1).get(4).toString());
            int p3 = Integer.parseInt(values.get(r1).get(5).toString());
            int gradesAverage = (p1+p2+p3)/3;
            
            if (studentAbscence > semesterLessons/4) {
                result.add(r1,abscence);
            } else if (gradesAverage < 50) {
                result.add(r1, grades);
            } else if (gradesAverage >= 50 && gradesAverage < 70) {
                List<Object> exam = Arrays.asList("Exame final", Math.round((100 - x)/2f));
                result.add(r1, exam);
            } else {
                result.add(r1, approved);
            }
        }
        } return result;
    }
    
    /**
     *
     * @param args
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        //Write calculated data to the same spreadsheet.
        String spreadsheetId = "18Mv4muqa2bBQy9BZZupbXiwT__h0RiLnn3W2QC2TlqA";
        String readRange = "engenharia_de_software!A4:H";
        String writeRange = "engenharia_de_software!G4:H";
        
        ValueRange body = new ValueRange().setValues(getSpreadSheetData(spreadsheetId,readRange));
        UpdateValuesResponse result = service().spreadsheets().values()
                .update(spreadsheetId, writeRange, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }
}

