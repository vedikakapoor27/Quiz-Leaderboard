import java.io.*;
import java.net.*;
import java.util.*;

public class Main{

    static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    static final String REG_NO = "RA2311050010025"; // <-- Put your reg number here

    public static void main(String[] args) throws Exception {

        // To track duplicates: "roundId_participant"
        Set<String> seen = new HashSet<String>();

        // To store total score per participant
        Map<String, Integer> scores = new LinkedHashMap<String, Integer>();

        // Step 1: Poll 10 times (poll 0 to 9)
        for (int poll = 0; poll < 10; poll++) {
            System.out.println("Fetching poll " + poll + "...");

            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;

            String body = sendGet(url);
            System.out.println("Response: " + body);

            // Step 2: Parse events manually
            int eventsStart = body.indexOf("\"events\"");
            if (eventsStart == -1) {
                System.out.println("No events in this response, skipping.");
            } else {
                int arrayStart = body.indexOf("[", eventsStart);
                int arrayEnd = body.lastIndexOf("]");
                String eventsArray = body.substring(arrayStart + 1, arrayEnd);

                String[] eventObjects = eventsArray.split("\\},\\s*\\{");

                for (String eventStr : eventObjects) {
                    eventStr = eventStr.replace("{", "").replace("}", "").trim();

                    String roundId = extractValue(eventStr, "roundId");
                    String participant = extractValue(eventStr, "participant");
                    String scoreStr = extractValue(eventStr, "score");

                    if (roundId == null || participant == null || scoreStr == null) {
                        continue;
                    }

                    int score = Integer.parseInt(scoreStr.trim());

                    // Step 3: Deduplicate using roundId + participant
                    String key = roundId + "_" + participant;

                    if (seen.contains(key)) {
                        System.out.println("  Duplicate found, skipping: " + key);
                    } else {
                        seen.add(key);
                        int current = scores.containsKey(participant) ? scores.get(participant) : 0;
                        scores.put(participant, current + score);
                        System.out.println("  Added: " + participant + " = +" + score + " (round " + roundId + ")");
                    }
                }
            }

            // Step 4: Wait 5 seconds before next poll
            if (poll < 9) {
                System.out.println("Waiting 5 seconds...\n");
                Thread.sleep(5000);
            }
        }

        // Step 5: Sort leaderboard by totalScore descending
        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<Map.Entry<String, Integer>>(scores.entrySet());
        Collections.sort(leaderboard, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });

        // Step 6: Print leaderboard
        System.out.println("\n========== LEADERBOARD ==========");
        int grandTotal = 0;
        for (Map.Entry<String, Integer> entry : leaderboard) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
            grandTotal += entry.getValue();
        }
        System.out.println("Total Score of all users: " + grandTotal);
        System.out.println("==================================\n");

        // Step 7: Build submit JSON manually
        StringBuilder leaderboardJson = new StringBuilder("[");
        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            leaderboardJson.append("{\"participant\":\"")
                    .append(entry.getKey())
                    .append("\",\"totalScore\":")
                    .append(entry.getValue())
                    .append("}");
            if (i < leaderboard.size() - 1) {
                leaderboardJson.append(",");
            }
        }
        leaderboardJson.append("]");

        String submitBody = "{\"regNo\":\"" + REG_NO + "\",\"leaderboard\":" + leaderboardJson + "}";

        System.out.println("Submitting: " + submitBody);

        // Step 8: Submit once
        String submitResponse = sendPost(BASE_URL + "/quiz/submit", submitBody);
        System.out.println("Submit Response: " + submitResponse);
    }

    // Simple GET request using HttpURLConnection (Java 8 compatible)
    static String sendGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }

    // Simple POST request using HttpURLConnection (Java 8 compatible)
    static String sendPost(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }

    // Helper to extract a value from a flat JSON string
    static String extractValue(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        String rest = json.substring(colonIndex + 1).trim();

        if (rest.startsWith("\"")) {
            int start = rest.indexOf("\"") + 1;
            int end = rest.indexOf("\"", start);
            return rest.substring(start, end);
        } else {
            int end = rest.indexOf(",");
            if (end == -1) end = rest.length();
            return rest.substring(0, end).trim();
        }
    }
}