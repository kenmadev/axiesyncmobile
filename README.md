<p align="center">
    <img src="https://i.imgur.com/skukI2q.png" style="width: 150px"/>
    </br>
    <b>Axie Sync Mobile Client</b>
    <br></br>
</p>

[![downloadsBadge](https://img.shields.io/npm/dt/axiesyncmobile?style=for-the-badge)](https://github.com/kenmadev/axiesyncmobile)
[![versionBadge](https://img.shields.io/npm/v/axiesyncmobile?style=for-the-badge)](https://github.com/kenmadev/axiesyncmobile)

Axie Sync Mobile is a [React Native](https://reactnative.dev/) project created for scholars to automatically sync their Axie Infinity battles logs to the cloud.

## Inspiration
When season 19 came out. Axie Infinity disabled the old battle logs where you can look up anyones battle history via Ronin address.
It was a useful feature specially for managers who were constantly checking the progress of their newly recruited scholars.
Axie Infinity replaced it by storing the battle history into the device as a `cache` file. The only way for others to view a battle is to share it via  the "Share" button.

This project let players sync their battle history into the cloud just by using the application. Which may help managers track their scholars just like the old days.
It's still unknown when is the new Battle logs API gonna come out. Until then, the Axie Sync project remains.

## Axie Project
- [axie-sync-server](https://github.com/kenmadev/axie-sync-server) - This handles all the request from pc and mobile clients. Store and query battle logs easily via the API.
- [axie-sync-pc](https://github.com/kenmadev/axie-sync-pc) - The PC client that sync Axie Infinity battle history to `axie-sync-server`. Only available for Windows.
- [axiesyncmobile](https://github.com/kenmadev/axiesyncmobile) - The Mobile client that sync Axie Infinity battle history to `axie-sync-server`. Only available for Android devices.

## How to use the application?
1. Download and open the app and follow the guidelines for first time usage
2. Your device file explorer should open. Look for Axie Infinity folder. Under `Android/data/com.axieinfinity.origin`. You know you're in the right folder if you see `cache` and `files` folders.
3. Tap the Sync button and minimize the app
4. Start playing some Axie battle

## How do I know its working?
First, ensure that the Axie Sync is in your notification tray with a message `Battle history is being monitored`. You can't close the notification while in sync mode. When a battle is completed, the message will update with the following
- `Recent battle is being sync`
- `Recent battle successfully synced`
- `Recent battle failed to sync`

## Do we have a client for PC?
Yes, visit [Axie Sync PC](https://github.com/kenmadev/axie-sync-pc) for more info. This is only for Windows.

## Do we have a version for iOS?
Sorry, we don't have. Android only.

## How does the cloud server works?
Check out the [Axie Sync Server](https://github.com/kenmadev/axie-sync-server) and get more information how the server handles the request coming from both clients.

## I'm a dev how can I contribute?
Axie Sync Mobile is a `React Native` project. A basic understanding of React Native is a must.
To contribute, just clone the repository and follow the instructions below.

First, clone and install the dependencies.

```bash
$ git clone https://github.com/kenmadev/axiesyncmobile
$ cd axiesyncmobile
$ yarn install
```

Run the development server
```bash
$ yarn run start
```

Install to an emulator or a real device
```bash
$ yarn run android
```

## Build the App
```bash
$ yarn run release
```
An `apk` file will be generated inside `./app/build/outputs/apk` folder

## Can I setup my own server for the logs?
Sure you can! Use the [Axie Sync Server](https://github.com/kenmadev/axie-sync-server) and host it yourself. Edit the `src/config.js` in this repo and look for `REMOTEAPI` key and replace it with your own remote API endpoint.

## Do you have a Discord?
Yup! Join our [Discord](https://discord.gg/3MQ63hSwqv) server.

## Donate
I do this open source work in my free time. If you'd like me to invest more time on it, please donate. Thank you in advance!
- BNB (BEP-20) or ETH (ERC-20) - `0xff66328040AeaD7ae1E7Ab5c858652cECD0a1ad2`
- WETH/SLP/AXS (RON) - `ronin:8374b56c6329815264dbfc2a139d259e95bf745d`
