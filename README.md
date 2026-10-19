# HighLife Client

Build:

```bat
gradlew.bat shadowJar
```

Put `client.jar`, `scripts/`, and `args.txt` in this folder, then run:

```bat
java -jar build\libs\local.jar run
```

If the args file is elsewhere:

```bat
java -jar build\libs\local.jar run client.jar scripts "C:\path\to\args.txt"
```

Defaults are `client.jar`, `scripts`, and `args.txt`.

The client starts directly and loads scripts from `scripts/`. Jagex login is still required.

Java 17 and Windows are required. Update `src/main/resources/versions.tsv` when the client version changes.


As always, this won't be updated. Don't ask for it. 

Join here for more [https://discord.gg/ezFHE4qXQG](https://discord.gg/ezFHE4qXQG)
