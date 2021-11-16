//cette premiere méthode est dedié pour visualiser les donées en sortie soit sur le moniteur série ou sur l'application android.
void serialOutput(){
     
  switch(outputType){ //la variable statique outputType est définié dans le fichier principale.
    
    case SERIAL_PLOTTER:  //Pour visaualiser les données dans le Traceur série de IDE Arduino
      Serial.print(BPM);   //les valeurs du BPM (Beat Per Minute), nombre de battements par minute
      Serial.print(",");   //virgule comme séparateur de données
      Serial.print(IBI);   //les valeurs de IBI (Inter Beats Interval) l'interval entre deux battements cardiaques
      Serial.print(",");  //virgule comme séparateur de données
      Serial.println(Signal); //les valeurs du signal ECG
      break;
      
    case ANDROID: //Pour visaualiser les données dans l'appliaction Android on les envoies sur Bluetooth
    
      //Pour envoyer ces données de l'arduino vers android on utilise le module bluetooth déja définie.
      BTserial.print('s');  //la lettre "s" qui precède chaque valeur de signal pour indiquer a l'app android qu'il sagit du signal
      //Pour poser ces données sur bluetooth on utilise la fct "print()"
      BTserial.print(floatMap(Signal,320,360,0,5),2); //la fct floatMap() est une fct définie par nous meme pour la conversion de données avant qu'elles sont envoyées sur bluetooth.
     //la fct floatMap()est de type float, c'est elle qui garantie le bon affichage du graphe ECG sur l'app android par la conversion de données envoyées.
     //voir le script de la fct ci-dessous pour plus détails.
      break;
    default:
      break;
  }

}


   // Tant que l'arduino recoie des battements de coeur, il fait des calculs spécifiques pour trouver les valeurs de BPM et IBI dans le fichier interrupt.
  // et puis il fait appel a cette fct pour choisir la façon de sortie de ces données soit sur le moniteur série soit l'app android.
 //Alors pour effectuer cette derniere il doit envoyer ces données sur bluetooth.
 
void serialOutputWhenBeatHappens(){
  switch(outputType){
    case ANDROID:
         BTserial.print('b');   //la lettre "b" pour indiquer qu'elles sont des valeurs BPM.
         BTserial.print(BPM);   //la valeur BPM.
         BTserial.print(',');   //séparateur virgule.
         BTserial.print('i');   //la lettre "i" pour indiquer qu'elles sont des valeurs IBI.
         BTserial.print(IBI);   //la valeur IBI
         BTserial.print('e');   //fin 
         break;
    default:
      break;
  }
}

//la fct floatMap() de type float dont on a déja parler. 
//**************valeurs d'enter *****************
//le parametre "x" pour la valeur du Signal.
//le parametre "inMin" pour la valeur maximal de l'onde du signal.
//le parametre "inMax" pour la valeur minimal de l'onde du signal.
//**************valeurs de sortie *****************
//elles representent aussi l'intervalle de variation de valeurs converties dans notre app android.
//le parametre "outMin" pour la valeur minimal de l'onde du nouveau signal sur android.
//le parametre "outMax" pour la valeur maximal de l'onde du nouveau signal sur android.

  float floatMap(float x, float inMin, float inMax, float outMin, float outMax){
  return (x-inMin)*(outMax-outMin)/(inMax-inMin)+outMin;
}
