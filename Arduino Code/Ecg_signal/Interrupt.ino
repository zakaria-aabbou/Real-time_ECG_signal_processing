
volatile int rate[10];                        // tableau pour contenir les dix dernières valeurs IBI
volatile unsigned long sampleCounter = 0;    // utilisé pour déterminer la synchronisation des impulsions
volatile unsigned long lastBeatTime = 0;    // utilisé pour trouver IBI
volatile int P =360;                       // utilisé pour trouver le pic de l'onde de pouls
volatile int T = 360;                     // utilisé pour trouver un creux dans l'onde de pouls
volatile int thresh = 340;               // utilisé pour trouver un moment instantané de battement de coeur
volatile int amp = 0;                   // utilisé pour maintenir l'amplitude de la forme d'onde d'impulsion
volatile boolean firstBeat = true;     // utilisé pour amorcer le tableau de taux, donc nous démarrons avec un BPM raisonnable
volatile boolean secondBeat = false;  // utilisé pour amorcer le tableau de taux, donc nous démarrons avec un BPM raisonnable


//Exemple de Calcul théorique du comptage:
//Le timer 2 sera utilisé en le faisant compter à la fréquence de 62500 Hz (fréquence d’horloge(16 MHz) divisée par 256).
//Un cycle d’horloge dure donc 16 µs (l’inverse de la fréquence). Pour avoir 500 ms (500000 µS), 
//il faut compter 500 000 µs / 16 µs = 31250 impulsions pour que cela réalise 500 000 µs=0.5s. Cette valeur est décomposable en 125 * 250.
//Le timer doit compter 250 fois pour déborder ; il suffit de le faire partir de la valeur 6.
//Chaque fois qu’il déborde, une variable compteur est incrémentée ; quand cette variable atteint 125,
//on a bien 500 ms qui se sont écoulées et on agit sur la LED.
//***********************************************************
//Dans le mode "CTC" ; le timer compte de 00 à TOP (qui vaut OCR2A soit 124) et lorsqu’il atteint cette valeur, il est remis à zéro et une interruption est générée,
//par le flag OCF2A en positionnant à 1 le bit OCIE2A qui est le bit 1 du registre TIMSK2.
//pour plus de details "https://www.locoduino.org/spip.php?article89"

void interruptSetup(){   
// Initialise Timer2 pour lancer une interruption tous les 2 ms.
//les différents registres sur 8bits de contrôle associés au timer.
  TCCR2A = 0x02;     //DÉSACTIVER PWM ( Modulation par Largeur d’Impulsion )SUR LES BROCHES NUMÉRIQUES 3 ET 11 ET PASSER EN MODE CTC (Clear Timer on Compare)
  TCCR2B = 0x06;    // NE PAS FORCER COMPARER,diviser la fréquence de base (62500 Hz) par 256 grace au prédiviseur en microcontroleur .(0x06 est 256 en decimal)c'est le rapport de devision
  OCR2A = 0X7C;     // RÉGLEZ LE HAUT DU COMPTE À 124 POUR UN TAUX D'ÉCHANTILLON DE 500 Hz
  TIMSK2 = 0x02;   // ACTIVER L'INTERRUPTION SUR LE MATCH ENTRE TIMER2 ET OCR2A
  sei();          // ASSUREZ BIEN QUE LES INTERRUPTIONS GLOBALS SONT ACTIVÉES
}


//Quand un événement interne (timer) ou bien externe (broche) demande une interruption au microcontrôleur .
//Celui-ci va s’interrompre dans son programme principal, puis va exécuter un sous-programme (on parle de routine d’interruption).
//Une fois que cette routine est exécutée, le microcontrôleur reprend son programme principal,
//là où il s’était arrêté, et continue son exécution.
//Pour cela, il a fallu que le microcontrôleur sauve différents registres sur une pile et les rétablisse en fin de routine d’interruption.

// C'EST LA ROUTINE DE SERVICE D'INTERRUPTION DE TIMER 2.
// Le Timer 2  s'assure que nous prenons une lecture toutes les 2 millisecondes

ISR(TIMER2_COMPA_vect){             // déclenché lorsque Timer2 compte jusqu'à 124
  cli();                           // désactiver les interruptions pendant que nous faisons cela
  Signal = analogRead(pulsePin);  // lire le capteur de pouls
  sampleCounter += 2;            // garde le temps en mS avec cette variable
  int N = sampleCounter - lastBeatTime;   // surveille le temps écoulé depuis le dernier battement pour éviter le bruit

    
// trouver le pic et le creux de l'onde de pouls 
  if(Signal < thresh && N > (IBI/5)*3){      // évite le bruit dichrotique en attendant 3/5 du dernier IBI
    if (Signal < T){                        // T est l'auge de l'onde
      T = Signal;                          // garde trace du point le plus bas de l'onde de pouls
    }
  }

// la condition de battement permet d'éviter le bruit
  if(Signal > thresh && Signal > P){     // P est le pic de l'onde cardiaque    
  
    P = Signal;                          // garde la trace du point le plus haut de l'onde de pouls   
  }                                      

// MAINTENANT, IL EST TEMPS DE CHERCHER LES BATTEMENTS DU CŒUR

// le signal augmente en valeur chaque fois qu'il y a une impulsion
  if (N > 250){                                    // évite le bruit haute fréquence
    
    if ( (Signal > thresh) && (Pulse == false) && (N > (IBI/5)*3) ){
      
      Pulse = true;                               // définir l'indicateur Pulse quand on pense qu'il y a une impulsion
      digitalWrite(blinkPin,HIGH);               // allume la broche 13 LED
      IBI = sampleCounter - lastBeatTime;       // mesure le temps entre les battements en mS
      lastBeatTime = sampleCounter;            // garde le temps pour la prochaine impulsion

      if(secondBeat){                         // si c'est le deuxième battement, si secondBeat == TRUE
        secondBeat = false;                  // efface l'indicateur secondBeat
        for(int i=0; i<=9; i++){            // amorce le total cumulé pour obtenir un BPM réaliste au démarrage
          rate[i] = IBI;
        }
      }

      if(firstBeat){                           // si c'est la première fois que nous trouvons un battement, si firstBeat == TRUE
        firstBeat = false;                    // effacer l'indicateur firstBeat
        secondBeat = true;                   // définir le deuxième indicateur de temps
        sei();                              // réactiver les interruptions
        return;                            // La valeur IBI n'est pas fiable, alors jetez-la
      }


      
// conserve un total cumulé des 10 dernières valeurs IBI
      word runningTotal = 0;                  // effacer la variable runningTotal

      for(int i=0; i<=8; i++){                // déplacer les données dans le tableau des taux et supprimez la valeur IBI la plus ancienne
        rate[i] = rate[i+1];                 
        runningTotal += rate[i];             // additionne les 9 valeurs IBI les plus anciennes
      }

      rate[9] = IBI;                        // ajoute le dernier IBI au tableau des taux
      runningTotal += rate[9];              // ajouter la dernière IBI à runningTotal
      runningTotal /= 10;                  // moyenne des 10 dernières valeurs IBI
      BPM = 60000/runningTotal;           // combien de temps peut tenir une minute? c'est la valeur BPM!
      QS = true;                          // définir l'indicateur d'auto quantifié (Quantified Self)
                                         // L' INDICATEUR QS N'EST PAS DÉGAGÉ À L'INTÉRIEUR DE CET ISR
    }
  }

  if (Signal < thresh && Pulse == true){    // lorsque les valeurs baissent, le rythme du battement est terminé
    digitalWrite(blinkPin,LOW);            // éteindre la broche 13 LED
    Pulse = false;                        // réinitialise le drapeau Pulse pour que nous puissions recommencer
    amp = P - T;                         // obtenir l'amplitude de l'onde d'impulsion
    thresh = amp/2 + T;                 // régler le seuil à 50% de l'amplitude
    P = thresh;                        // réinitialise ces derniers (P , T) pour la prochaine fois
    T = thresh;
  }

  if (N > 2500){                             // si 2,5 secondes passent sans un battement
    thresh = 340;                           // définir le seuil par défaut 340
    P = 360;                               // définir P par défaut 360
    T = 360;                              // définir T par défaut 360
    lastBeatTime = sampleCounter;        // mettre à jour le lastBeatTime
    firstBeat = true;                   // régler (firstBeat et secondeBeat) pour éviter le bruit quand on reprend le rythme cardiaque
    secondBeat = false;                 
  }

  sei();                                // activer les interruptions lorsque vous avez terminé!
}// fin isr
