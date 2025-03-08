 
 ///////////////// Android Client Data Parsing /////////////////
function parseClientData(data_String) {
    
    // Split the string by spaces
    const parts = data_String.split(' ');
    if (parts.length < 4) {
      console.error('Invalid data format');
      return null;
    }
  
    // Create and return struct with the data and properties
    return {
        device_type: parts[0],  // "I" for IoT or "C" for client, in use only if all data are sent to the same topic
        device_ID: parts[1],
        latitude: parseFloat(parts[2]),
        longitude: parseFloat(parts[3]),
    };
  }



  ///////////////// IoT Data Parsing /////////////////
  function parseIoTData(dataString) {
    // Split the string into an array using space as the delimiter
    const parts = dataString.split(' ');
    if (parts.length < 6) {
      console.error('Invalid data format');
      return null;
    }
  
    // Extract the common data
    const device_type = parts[0];
    const device_ID = parts[1];
    const device_serial = parts[2];
    const battery = parseFloat(parts[3]);
    const latitude = parseFloat(parts[4]);
    const longitude = parseFloat(parts[5]);
  
    let sensors = [];
    let riskLevel = null;
    // For IoT, the sensor type and value are in pairs of 2 and start after the 4 first values above
    if (parts.length > 7) { // The sensor data starts at index 4 until second to last element, check if there is any sensor data
      for (let i = 6; i < parts.length - 1; i += 2) {
        sensors.push({
          sensor: parts[i],
          value: parseFloat(parts[i + 1])
        });
      }
      
      riskLevel = parts[parts.length - 1];  // Last value is the risk level
    }
  
    // Create and return struct with the data
    return {
        device_type,
        device_ID,
        battery,
        device_serial,
        longitude,
        latitude,
        sensors,
        riskLevel
    };
  }
  