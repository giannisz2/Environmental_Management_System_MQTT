// Global const to store ids of devices (clients or sensors) that already have a marker on the map
// This is used to avoid creating multiple markers for the same device
const client_markers = {};
const iot_markers = [];
//let i=iot_markers.length;


// Map Initialization Function
let map;
function initMap() {
    const map_center = {lat:37.969, lng:23.769};  // Center of the map at the Department of Informatics and Telecommunications
       map = new google.maps.Map(document.getElementById("map"), {
        zoom: 16,
        center: map_center,
      });
}



////////////// IoT Overlays Functions //////////////

function gmaps_iot_overlays(IoT_Data){

  const device_id = IoT_Data.device_ID;
  const sensorLocation = {lat: IoT_Data.latitude, lng: IoT_Data.longitude};
  const riskLevel = IoT_Data.riskLevel;
  const battery = IoT_Data.battery;
  const lastcontact = Date.now();    // Save the time that this function was called for the specific device

  let isactive=0;
  let sensors_html=""; // If not initialised as an empty string, it print undfined in the info window
    
  if(IoT_Data.sensors === undefined || IoT_Data.sensors.length == 0) {
        isactive=0; 
        sensors_html = `<p>No sensors data available/received</p>`;
      }
    else isactive=1;

  for (let i = 0; i < IoT_Data.sensors.length; i++) { // Construct the sensors data html for the info window
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

  // Constaruct the content of the info window
  const contentString =`    
          <div id="content">
          <h1 id="firstHeading" class="firstHeading">IoT Device ID: ${device_id}</h1>
          <div id="bodyContent">
                <p><b>Battery level:</b> ${IoT_Data.battery}</p>
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
      iot_markers[device_id].lastcontact = lastcontact;
    }
    else{
      const marker = new google.maps.Marker({   // Create a marker for the sensor
      position: sensorLocation,
      map: map,
      title: "Sensor: " + device_id,
      icon: marker_icon,
      });

      const color_circle = new google.maps.Circle({   // Create a circle around the sensor that repreesents if the sensor is active (green) or not(red)
      strokeColor: "#0F0D00",
      strokeOpacity: 0.7,
      strokeWeight: 2,
      fillColor: active_color,
      fillOpacity: 0.70,
      map:map,
      center: sensorLocation,
      radius: 30,
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

    iot_markers[device_id] = { marker, color_circle, infowindow, riskLevel, lastcontact };
  }
  //i=iot_markers.length; //debugging
  //console.log(i);
  //console.log(iot_markers);
  gmaps_danger_zone_overlay(iot_markers); // Check if the danger zone overlay should be created 
  return;
}



function iot_deactivate(){
  for (let device_id in iot_markers) {
    if(iot_markers[device_id].lastcontact < Date.now() - 3000){
      iot_markers[device_id].color_circle.setOptions({ fillColor: "#D20103" });
      iot_markers[device_id].infowindow.close();
    }
  }
  return;
} 
setInterval(iot_deactivate, 2000); // Deactivate the sensor markers every 2 seconds

function iot_delete(){
  for (let device_id in iot_markers) {
    if(iot_markers[device_id].lastcontact < Date.now() - 8000){
      iot_markers[device_id].color_circle.setMap(null);
      iot_markers[device_id].infowindow.close();
      iot_markers[device_id].marker.setMap(null);
      delete iot_markers[device_id];
    }
  gmaps_danger_zone_overlay(iot_markers);
  }
  return;
}
setInterval(iot_delete, 2000); // Clean up the sensor markers every 8 seconds




let danger_zone;
function gmaps_danger_zone_overlay(iot_markers){
  //console.log("Running danger zone overlay");
  const marker_data = Object.values(iot_markers);
  //console.log(marker_data);

  if(marker_data.length == 2){
  if((marker_data[0].riskLevel == "High" || marker_data[0].riskLevel == "Medium") && (marker_data[1].riskLevel == "High" || marker_data[1].riskLevel == "Medium")){
    //console.log("In if Running danger zone overlay");
    if(danger_zone) return;
    // Create a LatLngBounds object and extend it to include both marker positions
    const edges = new google.maps.LatLngBounds();
      edges.extend(marker_data[0].marker.getPosition());
      edges.extend(marker_data[1].marker.getPosition());

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
 
  }}
  else if(danger_zone){   // If the danger zone overlay exists and risk is not above low in both sensors, remove the overlay
    console.log("Remove danger zone overlay");
    danger_zone.setMap(null);
    danger_zone = null;
  }
  else return
  

  return;
}

function danger_zone_overlay_cleanup(){ // Clean up the danger zone overlay every 6 seconds
  if(danger_zone){
    danger_zone.setMap(null);
    danger_zone = null;
    console.log("Remove danger zone overlay");
  }
  return;
}
setInterval(danger_zone_overlay_cleanup, 5000); 



////////////// Client Overlay Functions //////////////
function gmaps_client_overlay(android_data){

    const device_id = android_data.device_ID;
    const lastcontact = Date.now();   //save the time that this function was called for the specific device

    const device_position= {lat:android_data.latitude, lng:android_data.longitude};
    const marker_icon = { // Set marker icon to a mobile phone icon
      url: "assets/mobile-map.png", // image url
      scaledSize: new google.maps.Size(50, 50), // scaled size
  };

    if (client_markers[device_id]) {   // Check if the device already has a marker on the map
       // Update existing marker's position
        client_markers[device_id].client_marker.setPosition(device_position);
        return;
    }
    else{
      const client_marker = new google.maps.Marker({   // Create a marker for the device
          position: device_position,
          map: map,
          title: "Android Device ID: " + android_data.device_ID,
          icon: marker_icon
      });
      client_markers[device_id] = {client_marker, lastcontact};
  }

}

function client_cleanup(){    // Clean up the inactive client markers every 10 seconds
  for (let device_id in client_markers) {
    if(client_markers[device_id].lastcontact < Date.now() - 10000){
      client_markers[device_id].client_marker.setMap(null);
      delete client_markers[device_id];
    }
  }
  return;
}
setInterval(client_cleanup, 2000); 


window.initMap = initMap;


