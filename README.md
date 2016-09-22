# Statistiky pro pochod Z Brna do Brna

Kompiluje se do statického JS, běží celé v prohlížeči. Troška informací ke Scala.JS zde: https://www.scala-js.org/tutorial/basic/

## Běh lokálně

Některé prohlížeče (ehm, Chrome) mohou z bezpečnostních důvodů omezovat či zakazovat XMLHttpRequest na file:///. Pokud jde o omezení (jako ve Firefoxu), vadit to nejspíš nebude, protože CSV je ve stejném adresáři. Pokud jde o zákaz, je možné spustit jednoduchý HTTP server, například `python -m SimpleHTTPServer 8080`, a vyvíjet na něm.
