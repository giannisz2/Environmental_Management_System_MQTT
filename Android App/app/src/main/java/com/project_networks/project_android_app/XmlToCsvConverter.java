package com.project_networks.project_android_app;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

public class XmlToCsvConverter {

    public static void convertXmlToCsv(Context context, InputStream inputStream, String outputFileName) {
        XmlPullParser parser = Xml.newPullParser();

        try {
            parser.setInput(inputStream, null);

            // Prepare CSV headers
            StringBuilder csvData = new StringBuilder();
            csvData.append("X,Y\n"); // Header row

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName;

                if (eventType == XmlPullParser.START_TAG) {
                    tagName = parser.getName();

                    if ("vehicle".equals(tagName)) {
                        // Extract only x and y attributes from each <vehicle> element
                        String x = parser.getAttributeValue(null, "x");
                        String y = parser.getAttributeValue(null, "y");

                        // Append CSV row with x and y values
                        csvData.append(x).append(",").append(y).append("\n");
                    }
                }
                eventType = parser.next();
            }

            // Save CSV to a file
            saveCsvToFile(context, csvData.toString(), outputFileName);

        } catch (XmlPullParserException | IOException e) {
            Log.e("CSV Append", "Append succeeded", e);
        }
    }

    private static void saveCsvToFile(Context context, String csvData, String outputFileName) {
        try (FileOutputStream fos = context.openFileOutput(outputFileName, Context.MODE_PRIVATE);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            osw.write(csvData);
            Log.i("CSV Save", "File saved successfully");
        } catch (IOException e) {
            Log.e("CSV Save", "Error saving file", e);
        }
    }

}
