# Contributing
We appreciate any help from the community to improve the mod:

#### Bugs or ideas for new items:
Did you found a bug or do you have an idea how to improve the mod? We are happy to hear from you...

- [IRC Channel](https://qchat.rizon.net/?channels=electricalage)
- [Bug Tracker](https://github.com/Electrical-Age/ElectricalAge/issues)

### Translations
Is the mod not available in your language or are some translations missing?
Well you can change that by adding or modifying a translation:

- First you need to [Download](https://github.com/Electrical-Age/ElectricalAge/archive/1.7.10-MNA.zip)
or clone the Electrical Age source code:  
```sh
git clone https://github.com/Electrical-Age/ElectricalAge.git
```

- Create the new language file:  
If you add a new translation, create a new language file In the folder
*ElectricalAge/src/main/resources/assets/eln/lang* by using the UTF language symbol as the name of the file and the
file extension *.lang*. So for example the language file name for German would be *de_DE.lang*. This step is not
required if the language file already exists.

- Fill or update the language file:  
You need first to populate the language file with the actual strings to translate. There is a gradle task for that:
In the root folder (ElectricalAge), run the following gradle task:  
```sh
gradle updateLanguageFiles
```  
Use ```./gradlew``` or ```gradlew``` in place of gradle if you have gradle not installed.

- Add the actual translations:  
Using the text editor of you choice, you can now add translations to the file. The format is rather simple:  

```
#<ELN_LANGFILE_V1_1>

# ./src/main/java/mods/eln/Eln.java
mod.meta.desc=<TRANSLATION>

# ./src/main/java/mods/eln/misc/UtilsClient.java
hold shift=<TRANSLATION>
```

Some translation strings might contain placeholders for runtime arguments in order to include numbers or other runtime
objects into the sentence. These are identified by **%N$** whereas *N* is the number of the argument in the argument
list (at runtime). A translation string should include these placeholders too at the appropriate position in the text.

- Contribute the translation file:  
You can either create a pull request with the new language file or just create an issue with the new language file as
attachment in order to make the translation available to everyone.