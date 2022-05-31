package fr.enit.industryportal.fair2excelweb;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.enit.industryportal.fair2excelweb.converters.FairExcelConverter;
import fr.enit.industryportal.fair2excelweb.converters.QuestionsExcelConverter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * @author AbdelWadoud Rasmi
 * @github https://github.com/rasmi-aw
 * <p>
 * This servlet returns an excel sheet of the fair service results
 */
@WebServlet(name = "fair2excel", value = "/reporting")
public class Controller extends HttpServlet implements Config {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse httpResponse) throws ServletException, IOException {
        String ontologies = request.getParameter("ontologies");
        ontologies = ontologies == null || ontologies.isEmpty() ? "all" : ontologies;
        try {

            String fileName = "";
            String portal = "industryportal";

            if (portal != null && !portal.trim().isEmpty()) {
                portal = portal.trim();

                if (ontologies != null && !ontologies.trim().isEmpty()) {
                    ontologies = ontologies.toLowerCase(Locale.ROOT).equals("all") ? "all" : ontologies.toUpperCase(Locale.ROOT).trim();
                }


                if (fileName == null || fileName.trim().isEmpty()) {
                    fileName = "results-" + portal + ".xlsx";
                }

                String uri = String.format(FAIR_SERVICE_URL + "?portal=%s&ontologies=%s", portal, ontologies);
                JsonObject response = getJsonFromFairService(uri);


                System.out.println("Get Fair score of : " + ontologies);
                System.out.println("Use portal : " + portal);
                System.out.println("HTTP call : " + uri);

                if (!isResponseOK(response)) {
                    System.out.println("Error in fairness service : " + getErrorMessage(response));
                } else if (ontologies == null || ontologies.isEmpty()) {
                    System.out.println("Error in option 'ontologies' : it's empty");
                } else if (portal == null || portal.isEmpty()) {
                    System.out.println("Error in option 'portal' : it's empty");
                } else {
                    FairExcelConverter excelConverter = new FairExcelConverter(response);
                    excelConverter.toExcel(fileName, "FAIRness evaluation", true);
                    QuestionsExcelConverter questionsExcelConverter = new QuestionsExcelConverter(response);
                    questionsExcelConverter.toExcel(fileName, "Fairness questions evaluation ");
                    System.out.println("The file " + fileName + " was created ");
                    //
                    byte[] output = Files.readAllBytes(Paths.get(fileName));
                    httpResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    httpResponse.setContentLength(output.length);
                    httpResponse.addHeader("Content-Disposition", "attachment; filename=" + fileName);

                    httpResponse.getOutputStream().write(output);
                    return;
                }
            } else {
                System.out.println("portal error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR : " + e.getMessage());
        }
        //
        httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }


    /**
     *
     */
    private static String getErrorMessage(JsonObject response) {
        return response.getAsJsonObject("status").get("message").getAsString();
    }

    /**
     *
     */
    private static boolean isResponseOK(JsonObject response) {
        return response != null && response.getAsJsonObject("status").get("success").getAsBoolean();
    }

    /**
     *
     */
    private static JsonObject getJsonFromFairService(String URI) throws IOException {
        JsonObject out;
        URL url = new URL(URI);
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        Reader reader = new InputStreamReader(is);
        out = new GsonBuilder().create().fromJson(reader, JsonObject.class);
        return out;
    }

    /**
     *
     */
    private static Options getOptions() {
        final Options options = new Options();

        Option portalInstanceNameOpt = new Option("r", "repository-name", true,
                "Name of the ontology repository (agroportal, bioportal, stageportal).");
        portalInstanceNameOpt.setRequired(true);
        Option ontologyAcronymOpt = new Option("o", "ontology-acronyms", true,
                "Acronyms of the ontologies to evaluate (comma separated) or all.");
        ontologyAcronymOpt.setRequired(true);
        ontologyAcronymOpt.setValueSeparator(',');

        Option output = new Option("f", "output-file", true, "File output path");
        output.setRequired(false);

        options.addOption(portalInstanceNameOpt);
        options.addOption(ontologyAcronymOpt);
        options.addOption(output);
        return options;
    }
}
