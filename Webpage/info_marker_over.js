// Global const to store ids of devices (clients or sensors) that already have a marker on the map
// This is used to avoid creating multiple markers for the same device
const client_markers = {};
const iot_markers = [];
let i=iot_markers.length;

//const sensorsdata = [{id:0o1, lat:37.96809452684323, lng:23.76630586399502, temp:25, smoke:50, gas:123, uv:3, isactive:1},{id:0o2, lat:37.967212, lng:23.76528, temp:27, smoke:100, gas:444, uv:4, isactive:0},{id:0o3, lat:37.967779456380754, lng:23.767174897611685, temp:30, smoke:250, gas:555, uv:7, isactive:1}];

let map;
function initMap() {
    const map_center = {lat:37.969, lng:23.769};  // Center of the map at the Department of Informatics and Telecommunications
       map = new google.maps.Map(document.getElementById("map"), {
        zoom: 17,
        center: map_center,
      });
}

function gmaps_sensor_overlays(){ // to be deleted
  
  let rowIndex = 0;
  while(rowIndex < sensorsdata.length){
        
        const sensor_rd = sensorsdata[rowIndex];    //this gets the sersor data of the current row from the table
        const sensorLocation = {lat: sensor_rd.lat, lng: sensor_rd.lng};
        const active_color= sensor_rd.isactive ? "#7DDA58" : "#D20103";

        const contentString =`
            <div id="content">
            <h1 id="firstHeading" class="firstHeading">Sensor ID: ${sensor_rd.id}</h1>
            <div id="bodyContent">
                <p><b>Temprature:</b>${sensor_rd.temp}</p> 
                <p><b>Smoke:</b>${sensor_rd.smoke}</p> 
                <p><b>Gas:</b>${sensor_rd.gas}</p> 
                <p><b>UV:</b>${sensor_rd.uv}</p>
                <p><b>Is active:</b>${sensor_rd.isactive}</p>
            </div>
            </div>
            `;
        const marker = new google.maps.Marker({   //this creates a marker for the sensor
            position: sensorLocation,
            map: map,
            title: "Sensor: " + sensor_rd.id,
      });

        const color_circle = new google.maps.Circle({   //this creates a circle around the sensor that repreesents if the sensor is active (green) or not(red)
        strokeColor: "#FF0000",
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: "#7DDA58",
        fillOpacity: 0.40,
        map:map,
        center: sensorLocation.center,
        radius: 10000000,
      });

        const infowindow = new google.maps.InfoWindow({ //this creates an info window for the sensor
        content: contentString,
        ariaLabel: "Sensor: " + sensor_rd.id,
      });

        marker.addListener("click", () => {     //this opens the info window when the marker is clicked
        infowindow.open({
          anchor: marker,
          map,
        });
      });
      rowIndex++;
    }
}

function gmaps_sensor_overlays2(IoT_Data){

  const device_id = IoT_Data.device_ID;
  const sensorLocation = {lat: IoT_Data.latitude, lng: IoT_Data.longitude};
  const riskLevel = IoT_Data.riskLevel;
  
  let isactive=0;
  let sensors_html=""; // If not initialised as an empty string, it print undfined in the info window
    
  if(IoT_Data.sensors === undefined || IoT_Data.sensors.length == 0) {
        isactive=0; 
        sensors_html = `<p>No sensors data available/received</p>`;
      }
    else isactive=1;

  for (let i = 0; i < IoT_Data.sensors.length; i++) {
        sensors_html += `<p><b>${IoT_Data.sensors[i].sensor}:</b> ${IoT_Data.sensors[i].value}</p>`;
    }
  
  const active_color= isactive ? "#7DDA58" : "#D20103";

  let marker_icon = {  // Set the marker icon based on the risk level of the sensor
      url: "assets/location-pin.png", // image url
      scaledSize: new google.maps.Size(50, 50), // scaled size
    };
  if(riskLevel == "High") marker_icon = {
      url: "assets/high_risk_pin.png",
      scaledSize: new google.maps.Size(50, 50) // Need to set this parameter every time to avoid the gigantic marker icon size bug
    };
  if(riskLevel == "Medium") marker_icon={
      url: "assets/med_risk_pin.png",
      scaledSize: new google.maps.Size(50, 50)
    };
  const contentString =`
          <div id="content">
          <h1 id="firstHeading" class="firstHeading">IoT Device ID: ${device_id}</h1>
          <div id="bodyContent">
                ${sensors_html}
              <p><b>Is active: </b>${isactive}</p>
          </div>
          </div>
          `;

  if (iot_markers[device_id]) {   // Check if the device already has a marker on the map
        // Update existing marker's position and icon
        iot_markers[device_id].marker.setPosition(sensorLocation);
        iot_markers[device_id].marker.setIcon(marker_icon);
        
        // Update circle's location and color
        iot_markers[device_id].color_circle.setCenter(sensorLocation);
        iot_markers[device_id].color_circle.setOptions({ fillColor: active_color });
        
        // Update infowindow content
        iot_markers[device_id].infowindow.setContent(contentString);
        iot_markers[device_id].riskLevel = riskLevel;
    }
    else{
      const marker = new google.maps.Marker({   // Create a marker for the sensor
      position: sensorLocation,
      map: map,
      title: "Sensor: " + device_id,
      icon: marker_icon,
      });

      const color_circle = new google.maps.Circle({   // Create a circle around the sensor that repreesents if the sensor is active (green) or not(red)
      strokeColor: "#FF0000",
      strokeOpacity: 0.8,
      strokeWeight: 2,
      fillColor: active_color,
      fillOpacity: 0.70,
      map:map,
      center: sensorLocation,
      radius: 25,
      });

      const infowindow = new google.maps.InfoWindow({ // Create an info window for the sensor
      content: contentString,
      ariaLabel: "Sensor: " + device_id,
      });

      marker.addListener("click", () => {     // Opens the info window when the marker is clicked
      infowindow.open({
        anchor: marker,
        map,
        });
      });

    iot_markers[device_id] = { marker, color_circle, infowindow, riskLevel };
  }
  i=iot_markers.length;
  console.log(i);
  console.log(iot_markers);
  gmaps_danger_zone_overlay(iot_markers);
  return;
}
let danger_zone;
function gmaps_danger_zone_overlay(marker_data){
  console.log("Running danger zone overlay");
  console.log(marker_data);
  
  if((marker_data.length == 3) && (marker_data[1].riskLevel == "High" || marker_data[1].riskLevel == "Medium") && (marker_data[2].riskLevel == "High" || marker_data[2].riskLevel == "Medium")){
    console.log("In if Running danger zone overlay");
    if(danger_zone) return;
    // Create a LatLngBounds object and extend it to include both marker positions:
    const edges = new google.maps.LatLngBounds();
    edges.extend(marker_data[1].marker.getPosition());
    edges.extend(marker_data[2].marker.getPosition());

// Create a rectangle from the LatLngBounds
    danger_zone = new google.maps.Rectangle({
      bounds: edges,
      editable: false,
      draggable: false,
      strokeColor: '#FF0000',
      strokeOpacity: 0.8,
      strokeWeight: 10,
      fillColor: '#D20103',
      fillOpacity: 0.50,
  });

  // Set the rectangle on the map:
    danger_zone.setMap(map);
    console.log("Danger zone overlay created");
    console.log(danger_zone);
 
  }
  else if(danger_zone){
    console.log("Remove danger zone overlay");
    danger_zone.setMap(null);
    danger_zone = null;
  }
  else return
  

  return;
}


function gmaps_client_overlay(android_data){

    const device_id = android_data.device_ID;

    const device_position= {lat:android_data.latitude, lng:android_data.longitude};
    const marker_icon = { // Set marker icon to a mobile phone icon
      url: "assets/mobile-map.png", // image url
      scaledSize: new google.maps.Size(50, 50), // scaled size
  };

    if (client_markers[device_id]) {   // Check if the device already has a marker on the map
       // Update existing marker's position
        client_markers[device_id].setPosition(device_position);
        return;
    }
    else{
      
      const client_marker = new google.maps.Marker({   // Create a marker for the device
          position: device_position,
          map: map,
          title: "Android Device ID: " + android_data.device_ID,
          icon: marker_icon
      });
      client_markers[device_id] = client_marker;
  }
}


window.initMap = initMap;


