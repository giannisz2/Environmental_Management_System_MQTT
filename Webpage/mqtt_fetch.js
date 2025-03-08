function mqtt_fetch(){
    const brokerUrl = 'wss://test.mosquitto.org:8081'; // Public MQTT broker (WebSocket, encrypted for modern browsers, no credentials required)
    const teststring = "I 1 23.76630586399502 37.96809452684323 GAS 10.15 OD 0.15 Temp 45 High";
    const teststring2 = "I 1 23.76630586399502 37.96809452684323 GAS 10.15 OD 0.15 Temp 45 Medium";
    const teststring3 = "I 2 23.76730586399502 37.96809452684323 High";
    const teststring4 = "I 2 23.76730586399502 37.96809452684323 GAS 10.15 OD 0.15 Temp 45 Low";
    const teststring5 = "I 2 23.76930586399502 37.9689452684323 GAS 10.15 OD 0.15 Temp 45 High";
    // MQTT Android Client data fetch 
    const topic = 'project/js_data_android';
    const client = mqtt.connect(brokerUrl);

    client.on('connect', () => {
        console.log('Connected to MQTT broker');
        client.subscribe(topic, (err) => {
            if (!err) {
                console.log(`Subscribed to topic: ${topic}`);
            }
        });
    });


    client.on('message', (topic, message) => {
        console.log(`Received message from ${topic}: ${message.toString()}`);
        const client_string = message.toString();
        const client_data = parseClientData(client_string);
        gmaps_client_overlay(client_data);

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
        gmaps_sensor_overlays2(iot1_data);
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
        gmaps_sensor_overlays2(iot2_data);
    });

    iot2.on('error', (err) => {
        console.error('MQTT Error:', err);
    });

    // testing
    // const iot1_data = parseIoTData(teststring);
    // gmaps_sensor_overlays2(iot1_data);
    // const iot2_data = parseIoTData(teststring2);
    // gmaps_sensor_overlays2(iot2_data);
    // const iot3_data = parseIoTData(teststring3);
    // gmaps_sensor_overlays2(iot3_data);
    // const iot4_data = parseIoTData(teststring4);
    // gmaps_sensor_overlays2(iot4_data);
    // const iot5_data = parseIoTData(teststring5);
    // gmaps_sensor_overlays2(iot5_data);

}