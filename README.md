# Statistiky pro pochod Z Brna do Brna

Kompiluje se do statického JS, běží celé v prohlížeči. Troška informací ke Scala.JS zde: https://www.scala-js.org/tutorial/basic/

## Běh lokálně

1. Nainstaluj si [Activator](https://www.lightbend.com/activator/download) (popř. lze použít SBT)
2. activator ~run
3. Otevři http://localhost:9000/2016.html (případně jiný rok)

## Přidání ročníku

1. Uprav project/PageGenerator.scala
2. Pokud běží Activator, restartuj ho nebo použij příkaz reload.

## Export na web

a. Pouze pro Linux/MacOS: `./pack.sh` vygeneruje pack.zip
b. Kdekoliv: `activator stage` vygeneruje soubor server/target/scala-$scalaVersion/zbdb-stats-server_$scalaVersion-$version-web-assets.jar, ve kterém je adresář public.

## Verze formátu

Pokud je potřeba data z různých ročkíků zpracovávat různě, použije se jiná verze formátu. Verze formátu odpovídá ročkíku, od kterého se používá. Pokud uvidíme verze 2045, určitě se použila pro ročník 2045, ale možná i v dalších letech.
