# Auto Farmer (Fabric 1.21.4)

## Budowanie
1. Zainstaluj JDK 21.
2. W folderze projektu wykonaj `gradle wrapper --gradle-version 8.12` (albo skopiuj gradlew + folder gradle/ z oficjalnego fabric-example-mod).
3. `./gradlew build` -> gotowy plik w `build/libs/autofarmer-1.0.0.jar`.
4. Wrzuć jar do `mods/` razem z Fabric API 0.119.2+1.21.4.

## Użycie
- Receptura: iron_hoe + chest + 7 sztabek żelaza (wzór IHI / ICI / III).
- Postaw blok, kliknij PPM -> GUI.
- Wpisz współrzędne skrzynki z nasionami, skrzynki na plony i dwóch rogów obszaru (przycisk "Moja poz." wstawia Twoją pozycję; współrzędne zobaczysz na F3).
- Y obszaru = poziom roślin (blok NAD farmlandem). Farmland musi już być przygotowany i nawodniony.
- Wybierz nasiono przyciskiem, włącz pracę i zapisz.
