////////////////////////////////////////////////////////////////////////////
// This file is part of BlazeFly.                                         //
//                                                                        //
// BlazeFly is free software: you can redistribute it and/or modify       //
// it under the terms of the GNU General Public License as published by   //
// the Free Software Foundation, either version 3 of the License, or      //
// (at your option) any later version.                                    //
//                                                                        //
// BlazeFly is distributed in the hope that it will be useful,            //
// but WITHOUT ANY WARRANTY; without even the implied warranty of         //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the           //
// GNU General Public License for more details.                           //
//                                                                        //
// You should have received a copy of the GNU General Public License      //
// along with BlazeFly. If not, see <http://www.gnu.org/licenses/>.       //
////////////////////////////////////////////////////////////////////////////

package com.bradleyjh.blazefly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater implements Runnable {
	private Main main;
	private String thisVersion;

	public Updater(Main plugin, String version) {
		main = plugin;
		thisVersion = version;
	}

	public void run() {
		URL url;
		try {
			url = new URL("https://api.curseforge.com/servermods/files?projectIds=50224");
		} catch (MalformedURLException e) {
			return;
		}

		try {
			URLConnection conn = url.openConnection();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String response = reader.readLine();
			JSONArray array = (JSONArray) JSONValue.parse(response);

			if (array.size() > 0) {
				JSONObject latest = (JSONObject) array.get(array.size() - 1);
				String latestFile = (String) latest.get("name");

				// SomePlugin v2.3 = "230", SomePlugin v2.3.4 = "234"
				// means we can check if the newer file is a newer version
				String latestVersion = latestFile.replaceAll("\\D+", "");
				if (latestVersion.length() == 2) {
					latestVersion = latestVersion + "0";
				}
				thisVersion = thisVersion.replaceAll("\\D+", "");
				if (thisVersion.length() == 2) {
					thisVersion = thisVersion + "0";
				}

				if (Integer.parseInt(latestVersion) > Integer.parseInt(thisVersion)) {
					main.updateAvailable = latestFile;
					main.getLogger().info(latestFile + " is available for download!");
				}
			}
		} catch (IOException e) {
			return;
		}
	}
}