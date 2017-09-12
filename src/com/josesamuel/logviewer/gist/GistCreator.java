package com.josesamuel.logviewer.gist;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.http.impl.client.HttpClients.*;

/**
 * Handles the creation of gist
 */
public class GistCreator {

    public interface GistListener {
        void onGistCreated(String gistUrl);
    }

    private static final String GIST_API = "https://api.github.com/gists";

    /**
     * Create gist and returns the gist url
     */
    public void createGist(final String content, final GistListener gistListener) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = getCreateGistJson(content);
                    String result = callGistApi(json);
                    if (result != null) {
                        gistListener.onGistCreated(result);
                    }
                } catch (Exception ex) {
                }
            }
        });
    }

    /**
     * Call the API to create gist and return the http url if any
     */
    private String callGistApi(String gistJson) {
        try {
            CloseableHttpClient httpclient = createDefault();
            HttpPost httpPost = new HttpPost(GIST_API);
            httpPost.setHeader("Accept", "application/vnd.github.v3+json");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(gistJson));

            CloseableHttpResponse response = httpclient.execute(httpPost);

            HttpEntity responseEntity = response.getEntity();

            JsonObject result = (JsonObject) new JsonParser().parse(EntityUtils.toString(responseEntity));
            EntityUtils.consume(responseEntity);
            response.close();
            return result.getAsJsonPrimitive("html_url").getAsString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
        //return null;
    }

    /**
     * Returns the json for creating gist
     */
    private String getCreateGistJson(String content) {

        JsonObject gistObject = new JsonObject();
        gistObject.addProperty("description", "Shared log using Log Viewer  - https://josesamuel.com/logviewer/");

        JsonObject logDataObject = new JsonObject();
        logDataObject.addProperty("content", content);

        JsonObject fileDataObject = new JsonObject();
        fileDataObject.add("log", logDataObject);

        gistObject.add("files", fileDataObject);

        return gistObject.toString();
    }


}
