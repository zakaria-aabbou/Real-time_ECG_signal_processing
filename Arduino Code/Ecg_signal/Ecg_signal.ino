
#include <stdio.h>
#include <SoftwareSerial.h> // c'est une bibliothèque qui permet de faciliter le transfer de données entre la carte arduino et HC-05.

#define SERIAL_PLOTTER  2  //Définition d'une Constante pour le Moniteur Série.
#define ANDROID  3         //Définition d'une Constante pour L'application android.

//Définir le Module bluetooth, et affectation des broches de la carte arduino aux broches de transmition TX et de réception RX du module Bluetooth
SoftwareSerial BTserial(10,11); // Arduino RX(Broche Digital 10) - Bluetooth (Broche TX) | Arduino TX(Broche Digital 11) - Bluetooth (Broche RX)

//  Variables
int pulsePin = 0;                // Fil violet du capteur d'impulsions connecté à la broche analogique 0 (A0)
int blinkPin = 13;              // la LED incorporée, branchée sur la sortie numéro 13

int fadePin = 5;                  //utilise pour la LED pour le clignotement
int fadeRate = 0;                //utilisé pour atténuer la LED avec PWM sur fadePin

// Ici on a des variables Volatiles Utilisées dans le Service de Routine D'Interruption (ISR) dans le fichier (interrupt.ino)

volatile int BPM;                   //Un int qui contient l'analogique brut en 0. mis à jour tous les 2 ms
volatile int Signal;                // contient les données brutes entrantes
volatile int IBI = 600;             //un int qui contient l'intervalle de temps entre les battements!
volatile boolean Pulse = false;    // "True" lorsque le rythme cardiaque en direct de l'utilisateur est détecté. "Faux" quand ce n'est pas un "battre en direct".
volatile boolean QS = false;        // Devient True si il y'a un battement du coeur capté par l'arduino 

// decider si on veut envoyer les valeurs à l'application android ou bien au Traceur Série
//la variable statique utilisé dans le fichier (AllSerialHandling.ino)
static int outputType = ANDROID;
//static int outputType = SERIAL_PLOTTER;


void setup(){
  pinMode(blinkPin,OUTPUT);          // épingle qui clignotera à votre rythme cardiaque!
  pinMode(fadePin,OUTPUT);          // épingle qui clignotera à votre rythme cardiaque!
  Serial.begin(9600);              // Vitesse de reception de données sur le Moniteur Série en baud
  BTserial.begin(9600);           // Vitesse de transmition de données sur le Bluetooth HC-05
  interruptSetup();              //cette methode du fichier (interrupt.ino) est configuré pour lire le signal du capteur d'impulsions tous les 2 ms

}

//  Notre fonction boucle qui va s'occupeé des changements a chaque itération
void loop(){
  if (QS == true){     // Un battement de coeur a été trouvé
                       // BPM et IBI ont été déterminés
                       // Self quantifié "QS" vrai quand Arduino trouve un battement de coeur
        fadeRate = 255;         // Réalise l'effet de fondu LED
                                // Définissez la variable 'fadeRate' sur 255 pour atténuer la LED avec une impulsion
                                                             
        serialOutputWhenBeatHappens(); //C'est une méthode dans le fichier (AllSerialHandling.ino)qu'on l'expliquera après, qui va etre appelé si il y a un battement du coeur.
        
        QS = false;                      // réinitialise l'indicateur Quantified Self pour la prochaine fois
  }
  
  serialOutput();//C'est une 2ème méthode dans le fichier (AllSerialHandling.ino) qu'on l'expliquera après aussi, qui va etre appelé si il y a un signal capté par le sensor.
  
  delay(20);  //c'est une méthode arduino pour prendre une pause de 20 ms après que la boucle entre dans une autre itération.
  
  ledFadeToBeat();                      // Réalise l'effet de fondu LED                                      
}


//c'est la methode qui s'occupe des effets du LED
void ledFadeToBeat(){
    fadeRate -= 15;                         // définir la valeur de fondu des LED
    fadeRate = constrain(fadeRate,0,255);  // Empêche la valeur de fondu des LED de devenir négative!
    analogWrite(fadePin,fadeRate);          // LED s'estompe (lumiere faible)
  }
