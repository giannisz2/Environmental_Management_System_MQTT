1. Σταθερές και Μεταβλητές
   BROKER_URL: Η διεύθυνση του MQTT broker.
   TOPIC_SUB_ANDROID_APP, TOPIC_SUB_IOT1, TOPIC_SUB_IOT2: Τα topics που ακούει ο server.
   TOPIC_PUB_ANDROID_APP, TOPIC_PUB_JS1, TOPIC_PUB_JS2, TOPIC_PUB_ANDROID_JS: Τα topics στα οποία δημοσιεύει ο server.
   CLIENT_ID: Ένα μοναδικό αναγνωριστικό για τον MQTT client.
   lat1, lon1, lat2a, lon2a, lat2b, lon2b, distance: Γεωγραφικά δεδομένα και η απόσταση μεταξύ τους.
   eventOD1, eventGas1 κτλ.: Flags που δηλώνουν αν έχουν εντοπιστεί συμβάντα για διαφορετικούς αισθητήρες.
   measurement: Μέτρηση από έναν αισθητήρα.
   keepRunning: Χρησιμοποιείται για τη διατήρηση του loop λειτουργίας του server.

2. main Μέθοδος
   Εκκίνηση Server: Η μέθοδος serverStart ξεκινάει τον MQTT client σε ξεχωριστό thread.
   Χειρισμός Εισόδου Χρήστη: Το πρόγραμμα περιμένει από τον χρήστη να πατήσει Enter για να σταματήσει ο server (keepRunning = false).
   Αποσύνδεση: Όταν ο χρήστης δώσει το σήμα, ο server αποσυνδέεται από τον broker.

3. serverStart Μέθοδος
   Εκκινεί ένα νέο thread για να χειριστεί τον server.
   Συνδέεται στον MQTT broker και κάνει subscribe στα topics TOPIC_SUB_ANDROID_APP, TOPIC_SUB_IOT1, και TOPIC_SUB_IOT2.
   Ο server συνεχίζει να λειτουργεί όσο η μεταβλητή keepRunning είναι true.

4. getMqttClient Μέθοδος
   Δημιουργεί και επιστρέφει έναν MqttClient.
   Καθορίζει έναν callback handler που:
   connectionLost: Αντιμετωπίζει την απώλεια σύνδεσης με το broker.
   messageArrived: Επεξεργάζεται μηνύματα που λαμβάνονται από διαφορετικά topics.
   deliveryComplete: Επιβεβαιώνει ότι ένα μήνυμα έχει παραδοθεί.

5. Επεξεργασία Δεδομένων από Topics
   i. TOPIC_SUB_ANDROID_APP
   Επεξεργάζεται δεδομένα που περιλαμβάνουν:
   deviceID: Αναγνωριστικό συσκευής.
   lat1 και lon1: Συντεταγμένες.
   Εμφανίζει τα δεδομένα στην κονσόλα.
   ii. TOPIC_SUB_IOT1 και TOPIC_SUB_IOT2
   Επεξεργάζεται δεδομένα από αισθητήρες.
   Τα δεδομένα περιλαμβάνουν:
   Συντεταγμένες (lat2a, lon2a ή lat2b, lon2b).
   Ζεύγη "αισθητήρας - μέτρηση" (π.χ., OD 0.2).
   Οι αισθητήρες που ελέγχονται είναι:
   OD (Optical Density)
   GAS
   TEMP
   UV
   Ελέγχει αν οι μετρήσεις είναι έγκυρες και αν ξεπερνούν κάποια thresholds, για να εντοπίσει events.

6. Υπολογισμός Απόστασης και Επιπέδου Κινδύνου
   Υπολογίζει την απόσταση μεταξύ του χρήστη (lat1, lon1) και του σημείου του συμβάντος (lat2a, lon2a ή lat2b, lon2b).
   Υπολογίζει ένα "risk level" (High, Moderate) ανάλογα με τα ενεργά συμβάντα.
   Δημοσιεύει μήνυμα στο TOPIC_PUB_ANDROID_APP με το επίπεδο κινδύνου και την απόσταση.
   Καταγράφει τα δεδομένα στη βάση μέσω της DatabaseConnection.
   Δημοσιεύονται τα δύο μηνύματα στο frontend του server που χρειάζονται για την οπτικοποίηση δεδομένων. 

   Άλλες Παρατηρήσεις
   • Ο server τρέχει σε ξεχωριστό thread, επιτρέποντας ασύγχρονη λειτουργία.
   • Μπορεί να επεξεργάζεται δεδομένα από διαφορετικούς αισθητήρες και να προσαρμόζεται στις απαιτήσεις κάθε topic.
