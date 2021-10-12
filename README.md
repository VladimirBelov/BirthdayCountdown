# BirthdayCountdown

Android application: Events list and widgets for all your contacts dates: birthdays, weddings etc

## Introduction

List of your memorable dates and events (free and no ads).

Various event widgets in the form of scalable photo cards and lists. Individual settings for each widget.

Customizable notifications for upcoming events.

Quiz about the events of your contacts: when he was born? how old will he be? what month is her birthday?

Supported event types (from your contacts):
	1. Birthdays
	2. Wedding anniversaries
	3. Other events
	4. Custom events (name days, death anniversaries, ...)
	5. Round days for events (5k, 10k, 15k ... days)
	6. Calendar events (contacts birthdays, holidays, etc)
	7. Facebook events (via calendar events, see instructions in Settings->"How to" guide)
	8. Events and birthdays from local files (DarkBirthday Widget syntax, see "How to" guide for file format, file with events can be downloaded from https://4pda.to/forum/index.php?showtopic=939391 )

Supported accounts for events:
	1. Google
	2. Skype 7.x and earlier, Skype Lite
	3. Xiaomi
	4. VK
	5. Samsung
	6. Asus
	7. MS Exchange (via Gmail)
	8. IBM Verse (IBM Notes Traveler)
	9. Huawei

Special program features:	
	1. For wedding anniversaries displaying name of anniversary - Paper, Cotton, Leather...
	2. When searching for events, you can use any information from the contact card. To search OR, enter data separated by commas, to search AND, enter data by combining them with a "+"
	3. Current age or age at death (2 events are needed - birthday and date of death)
	4. Events types and display parameters can be configured in Settings menu
	5. Several color application themes
	6. If the contact does not have a photo, the image will be substituted automatically according to age and gender (calculated by first name and second name)
	7. Switching on sign of the zodiac and the zodiacal year is located in the application settings
	8. The common "problems" are explained in the "Questions and Answers" section: why notifications do not work, how to import events from social networks, etc.
 
Special widgets features: 
	1. Data displaying for every event: full name (or event date), days before event, age, event type icon
	2. For first events there is a quick access for contact card, click by last one opens events list
	3. Nearest events (1 or 2 days) are colored by yellow (customizable)
	4. Age for today events is colored by green (customizable). Or full name if age is undefined

<font color="#ff0000">The program does not change anything in system or user data, only reads data from your address book, calendars or local files. All events are set in contact cards, calendars or files.</font>

Note: standard Android Contacts application may not allow custom event date w/o year. To set this kind of events (for example, for name days) use 3d party contacts application.

## Installation

https://play.google.com/store/apps/details?id=org.vovka.birthdaycountdown

## Configuration

Create keystore.properties with the following info:

keyAlias='...'
keyPassword='...'
storeFile='...'
storePassword='...'

