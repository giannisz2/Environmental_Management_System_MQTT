ANDROID APP

Αρχικα, υπαρχουν 4 κλασεις που χωριζουν τις δουλειες του android app. 1) MainActivity, 2) ManualMode, 3) NetworkChecking, 4)XmlToCsvConverter.
Θα αναλυθουν ολες παρακατω.

1)Ξεκινάω στο MainActivity Class με τον ορισμο διάφορων μεταβλητων που θα χρησιμοποιήσω μεσα σε αρκετές μεθοδους, επομενως πρεπει το scope να να ειναι σε ολο το αρχειο.

Στην onCreate() ξεκιναω με αρχικοποιηση του menu toolbar που περιεχει το item Manual Mode(νεο activity) και Exit που σε βγαζει απο την εφαρμογη.
Επειτα χρησιμποποιω την getIntExtra() για να περασω την μεταβλητη manual_mode_seconds απο το αλλο activity σε αυτο. Αν η συναρτηση δεν βρει το key SECONDS_KEY
τοτε βαζει ως default τιμη την τιμη 0. Επειτα καλω την LocationServices.getFusedLocationProviderClient(this) που περνιέται στην μεταβλητη fusedLocationClient η οποια δρα σαν location provider
για τα GPS δεδομενα της συσκευης σε περιπτωση αυτοματου ελεγχου για την τοποθεσια. Επειτα καλειται η setupLocationCallback() η οποια setαρει ενα LocationCallback() για να δεχεται ασυγχρονα updates στην τοποθεσια
οσο κινειται η συσκευη στον χωρο. Μετά καλείται η checkLocationPermission() η οποια ελεγχει αν υπαρχουν τα permissions - αν δεν υπαρχουν ρωταει τον χρηστη για την ενεργοποιηση τους, αλλιως καλει την getLocation() η οποια ενημερωνει το location.
Επισης υπαρχουν κι αλλες 3 συναρτησεις callbacks για την τοποθεσια: η onRequestPermissionResult() που ελεγχει αν εχει γινει αποδεκτο το permission αλλιως εμφανιζει ενα toast message, η onResume() οπου συνεχιζει τα location updates και η onPause() που τα σταματαει αν πχ αλλαξει το Activity.
Γυρνάμε στην onCreate(). Επειτα αρχικοποιειται ο networkMonitor και καλειται η startChecking() οπου ελεγχει συνεχως αν ο χρηστης ειναι online. Αν δεν ειναι, του εμφανιζει ενα message που τον παραπεμπει στα settins για να το ενεργοποιησει.
Επειτα καλειται η random.nextInt() οπου επιλεγεται τυχαια ο αριθμος 0 η 1 για την τυχαια επιλογη του xml αρχειου για την τοποθεσια manually τωρα.
Αφου επιλεχθει το αρχειο τυχαια, καλειται η convertXmltoCsv() που δημιουργει ενα αρχειο csv και κανει append τα δεδομενα που θελουμε απο το xml. Θα αναλυθει παρακατω.
Επειτα αρχικοποιειται το mySwitch() που δινει στον χρηστη την δυνατοτητα να επιλεξει αν η αποστολη δεδομενων θα γινει manually ή automatic. Ετσι οπως εχει υλοποιηθει, ο χρηστης ανα πασα στιγμη μπορει
να αλλαξει τον τροπο αποστολης. Πρακτικα, ο setOnCheckedListener() του switch κανει το εξης: Υπαρχουν 2 threads runnables που κανουν τις διαφορετικες δουλειες. Το ενα στελνει manually, το αλλο αυτοματα.
Η λογικη ειναι η εξης: Αν ειναι checked το switch, σταματα το csvPublishRunnable και ξεκινα το automaticPublishRunnable, αν δεν ειναι, το αντιστροφο. Προεπιλεγμενη επιλογη ειναι το switch να ειναι checked.

Στην onCreateOptionsMenu() απλα υπαρχει το action να γινεται inflate το menu ολις ο χρηστης παταει τις τρεις τελιτσες πανω δεξια.

Η connectClicked() κανει την βασικοτερη δουλεια για την MQTT συνδεση. Αρχικα παιρνω τα δεδομενα που τοποθετει ο χρηστης στα EditTexts ipAdress και portString,
και ελεγχω αν δεν ειναι empty. Αν δεν ειναι, τσεκαρω αν ο χρηστης εχει δωσει σωστο αριθμο port απο 1 μέχρι 65535 που ειναι ο μεγιστος αριθμος available port.
Αν περασει και αυτον τον ελεγχο τοτε δημιουργω το τελικο url με αυτον τον τροπο:  "tcp://" + ipAddress + ":" + portNumber.
Επειτα ξεκιναω ενα UiThread() (για να σιγουρευτουμε οτι οτι αλλαγες γινουν στο ui θα γινουν απο το main ui thread) οπου θα setαρει το connection.
Μεσα στο try clause Setαρψ τα options, κανω generate ενα randomID για τον client και setαρω τα callbacks για το mqtt service. Τα callbacks ειναι τα εξης:
connectionLost() να εμφανιζει μηνυμα σε περιπτωση που χαθει η συνδεση, deliveryComplete που δεν κανει τιποτα αλλα αφου προ υπαρχει το αφησα εκει, 
και η messageArrived() οπου δεχεται το μηνυμα απο ενα topic που ειναι subscribed η συσκευη, κανει split τα δεδομενα σε riskLevel και distance - εδω εχει γινει συμβαση οτι ετσι θα ερχονται απο τον server,
και μετα καλει την handleRiskAlert(riskLevel, distance) οπου αναλογα τξ riskAlert, καλει αντιστοιχα την showAlert με αλλες παραμετρους οπου η συγκεκριμενη setαρει και εμφανιζει τις ειδοποιησεις σε περιπτωση που εχουμε event.
Αφου γινει η συνδεση, εμφανιζεται ενα μηνυμα και το anroid κανει subscribe στο topic του server για να δεχεται απο αυτον μηνυματα.ν Επειτα καλειται η startLocationPublishing() Που κανει ολη την δουλεια publish των δεδομενων.

Η συγκεκριμενη ξεκιναει δυο runnables (οπου λειτουργουν καθε ενα δευτερολεπτο) οπου καλουν την αντιστοιχη συναρτηση για publish δεδομενων με την λογικη που ξηγηθηκε πριν στο switch. Αναλογα με το state του switch ξεκιναει και τον αναλογο thread.
Το automaticPublishRunnable καλει την publishCoordinates() οπου απλα κανει publish τα δεδομενα, Ενω το csvPublishRunnable καλει πρωτα την publishFromCsvFile οπου διαβαζει ανα γραμμη το csv αρχειου που δημιουργηθηκε πριν, κανει extract τις τιμες x και y και
επειτα καλει την publishCoordinates() που κανει publish τις συντεταγμενες και το deviceID.

Τελος στο MainActivity class υπαρχει και η disconnectClicked() η οποια τρερματιζει τα runnables και την συνδεση με τον MQTT broker.


2)ManualMode ειναι το δευτερο activity που ειναι accesible απο το menu σε περιπτωση που ο χρηστης εχει επιλεξει publish με csv οπου απλα επιλεγεται ποσα δευτερολεπτα και ποσα δειγματα θα σταλθουν
με τον manual τροπο, μολις ο χρηστης πατησει το κουμπει set, η συναρτηση getIntent().getIntExtra() θα επιστρεψει στο main activity αυτην την τιμη που επελεξε ο χρηστης.

3)NetworkChecking, εδω δημιουργειται ενας handler ο οποιος τσεκαρει καθε 10000 ms αν υπαρχει συνδεση στο δικτυο. Αν δεν υπαρχει εμφανιζει ειδοποιηση στον χρηστη και τον παραπεμπει
στο να συνδεθει στο διαδικτυο.

4)XmlToCsvConverter, εδω γινεται ολη η δουλεια του conversion απο xml σε csv χρησιμοποιωντας τα capabilities του XmlFullParser.
