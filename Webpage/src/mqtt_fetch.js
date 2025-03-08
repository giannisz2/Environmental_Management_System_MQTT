function mqtt_fetch(){
    const brokerUrl = 'wss://test.mosquitto.org:8081'; // Public MQTT broker (WebSocket, encrypted for modern browsers, no credentials required)

    
    // MQTT Android Client data fetch 
    const topic = 'project/js_data_android';
    const client = mqtt.connect(brokerUrl);

    client.on('connect', () => {        // Connect to the MQTT broker and subscribe to the topic
        console.log('Connected to MQTT broker');
        client.subscribe(topic, (err) => {
            if (!err) {
                console.log(`Subscribed to topic: ${topic}`);
            }
        });
    });


    client.on('message', (topic, message) => {    // Run hen a message is received
        console.log(`Received message from ${topic}: ${message.toString()}`);
        const client_string = message.toString();   // Convert the message to a string
        const client_data = parseClientData(client_string);  // Parse the string to a struct
        if(client_data== null) return;
        gmaps_client_overlay(client_data);  // Call the function to create or update the marker on the map

    });

    client.on('error', (err) => {
        console.error('MQTT Error:', err);
    });
  

    // MQTT IoT 1 data fetch 
    const topic_iot1 = 'project/js_data1';
    const iot1 = mqtt.connect(brokerUrl);

    iot1.on('connect', () => {
        console.log('Connected to MQTT broker');
        iot1.subscribe(topic_iot1, (err) => {
            if (!err) {
                console.log(`Subscribed to topic: ${topic_iot1}`);
            }
        });
    });

    iot1.on('message', (topic_iot1, message) => {
        console.log(`Received message from ${topic_iot1}: ${message.toString()}`);
        const iot1_string = message.toString();
        const iot1_data = parseIoTData(iot1_string);
        if(iot1_data == null) return;
        gmaps_iot_overlays(iot1_data);
    });

    iot1.on('error', (err) => {
        console.error('MQTT Error:', err);
    });


    // MQTT IoT 2 data fetch 
    const topic_iot2 = 'project/js_data2';
    const iot2 = mqtt.connect(brokerUrl);

    iot2.on('connect', () => {
        console.log('Connected to MQTT broker');
        iot2.subscribe(topic_iot2, (err) => {
            if (!err) {
                console.log(`Subscribed to topic: ${topic_iot2}`);
            }
        });
    });

    iot2.on('message', (topic_iot2, message) => {
        console.log(`Received message from ${topic_iot2}: ${message.toString()}`);
        const iot2_string = message.toString();
        const iot2_data = parseIoTData(iot2_string);
        if(iot2_data == null) return;
        gmaps_iot_overlays(iot2_data);
    });

    iot2.on('error', (err) => {
        console.error('MQTT Error:', err);
    });

}