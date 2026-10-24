# TVHsync

TVHsync ist ein Plugin für [TV-Browser](https://www.tvbrowser.org/), mit dem sich die Aufnahmen eines
[tvheadend](https://tvheadend.org/)-Servers direkt aus TV-Browser heraus programmieren und verwalten lassen.

*TVHsync is a TV-Browser plugin to manage the timers and recordings of a tvheadend server.*

## Herkunft

TVHsync ist ein Fork von **[Lazy Bones](https://github.com/hampelratte/lazybones)** von Henrik Niehaus,
dem Plugin zur Steuerung eines VDR. Die komplette Oberfläche (Zeitleiste, Timerliste, Aufnahmeliste,
Kanalzuordnung, Konflikterkennung, Zuordnung der Timer zu TV-Browser-Sendungen) stammt aus Lazy Bones.
Ersetzt wurde die Anbindung: Statt SVDRP spricht TVHsync mit der HTTP/JSON-API von tvheadend.
Teile des Datenmodells stammen aus [svdrp4j](https://github.com/hampelratte/svdrp4j), ebenfalls von Henrik Niehaus.

TVHsync ist kein offizielles Projekt von Lazy Bones und wird von dessen Autoren weder unterstützt noch empfohlen.

## Funktionen

- Aufnahmen aus dem Kontextmenü von TV-Browser anlegen, bearbeiten und löschen
- Timer werden mit dem EPG-Eintrag von tvheadend verknüpft, damit tvheadend Zeitverschiebungen folgt
- Vor-/Nachlaufzeit, Priorität, Aufbewahrungsdauer und DVR-Profil pro Timer (die Ablage der Dateien regelt das DVR-Profil)
- Zeitleiste und Liste aller geplanten Aufnahmen, auch der von Autorec/Timerec erzeugten
- Liste der fertigen Aufnahmen: abspielen, umbenennen, löschen
- Sender und Aufnahmen über die Stream-URLs von tvheadend mit einem externen Player (z. B. mpv, vlc) abspielen
- Anzeige des freien Speicherplatzes

Nicht übernommen wurden Funktionen, die es bei tvheadend nicht gibt: Fernbedienung, Screenshots, VPS
und sich wiederholende Timer (dafür gibt es in tvheadend Autorec und Timerec).

## Voraussetzungen

- TV-Browser 4.2.3 oder neuer, Java 11 oder neuer
- tvheadend 4.2 oder neuer. Der verwendete Benutzer braucht DVR-Rechte.

## Installation

1. `TVHsync.jar` in das Plugin-Verzeichnis von TV-Browser kopieren, z. B.
   - `~/.tvbrowser/<version>/plugins`
   - Flatpak: `~/.var/app/org.tvbrowser.TVBrowser/.config/tvbrowser/<version>/plugins`
2. TV-Browser neu starten.
3. In den Einstellungen von TVHsync die URL des Servers eintragen (z. B. `http://192.168.1.10:9981`),
   eventuell Benutzer und Passwort, und mit „Verbindung testen“ prüfen.
4. Im Reiter „Sender“ die TV-Browser-Sender den tvheadend-Kanälen zuordnen.

Das Passwort wird wie alle Plugin-Einstellungen im Klartext in den TV-Browser-Einstellungen gespeichert.

## Bauen

Benötigt werden ein JDK (11 oder neuer) und Apache Ant. In `build.properties` muss `tvbrowser.runtime.dir`
auf das Verzeichnis zeigen, das `tvbrowser.jar` enthält.

```
ant jar       # erzeugt build/TVHsync.jar
ant install   # kopiert das Jar nach plugin.install.dir
```

## Lizenz

BSD-Lizenz wie Lazy Bones, siehe [LICENSE](LICENSE). Die Lizenzen der verwendeten Bibliotheken liegen in `licenses/`.
