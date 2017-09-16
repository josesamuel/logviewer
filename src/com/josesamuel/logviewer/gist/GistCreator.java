package com.josesamuel.logviewer.gist;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import com.josesamuel.logviewer.util.SingleTaskBackgroundExecutor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static org.apache.http.impl.client.HttpClients.createDefault;

/**
 * Handles the creation of gist and returns the gist share url back through listener
 */
public class GistCreator {

    /**
     * Listener to get the share url back
     */
    public interface GistListener {
        void onGistCreated(String gistUrl);

        void onGistFailed(String ex);
    }

    private static final String GIST_API = "https://api.github.com/gists";

    /**
     * Create gist and returns the gist url
     */
    public void createGist(Project project, final String content, final GistListener gistListener) {
        SingleTaskBackgroundExecutor.executeIfPossible(project, progressIndicator -> {
            try {
                String json = getCreateGistJson(content);
                UIUtil.invokeLaterIfNeeded(() -> progressIndicator.setFraction(.4));
                String result = callGistApi(json, gistListener);
                UIUtil.invokeLaterIfNeeded(() -> progressIndicator.setFraction(.8));
                if (result != null) {
                    gistListener.onGistCreated(result);
                } else {
                    gistListener.onGistFailed("Failed to share");
                }
                UIUtil.invokeLaterIfNeeded(() -> progressIndicator.setFraction(1));
            } catch (Exception ex) {
                gistListener.onGistFailed(ex.getMessage());
            }

        });
    }

    /**
     * Call the API to create gist and return the http url if any
     */
    private String callGistApi(String gistJson, GistListener listener) {
        try {
            CloseableHttpClient httpclient = createDefault();
            HttpPost httpPost = new HttpPost(GIST_API);
            httpPost.setHeader("Accept", "application/vnd.github.v3+json");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(gistJson, ContentType.APPLICATION_JSON));

            CloseableHttpResponse response = httpclient.execute(httpPost);

            HttpEntity responseEntity = response.getEntity();
            JsonObject result = (JsonObject) new JsonParser().parse(EntityUtils.toString(responseEntity));

            EntityUtils.consume(responseEntity);
            response.close();

            httpclient.close();
            return result.getAsJsonPrimitive("html_url").getAsString();
        } catch (Exception ex) {
        }
        return null;
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
