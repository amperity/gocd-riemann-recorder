.PHONY: all clean uberjar plugin docker-install
.SUFFIXES:

version := $(shell grep defproject project.clj | cut -d ' ' -f 3 | tr -d \")
plugin_name := gocd-riemann-recorder-$(version)-plugin.jar

uberjar_path := target/uberjar/gocd-riemann-recorder-$(version)-standalone.jar
plugin_path := target/plugin/$(plugin_name)
install_path := gocd/server/plugins/external/$(plugin_name)

all: plugin

clean:
	rm -rf target

$(uberjar_path): project.clj $(shell find resources -type f) $(shell find src -type f)
	lein uberjar

uberjar: $(uberjar_path)

$(plugin_path): $(uberjar_path)
	@mkdir -p target/plugin
	cd target/plugin; jar xf ../../$(uberjar_path)
	rm -f target/plugin/*.class
	rm -f target/plugin/org/apache/thrift/transport/TFileTransport*.class
	find target/plugin -type f -path 'target/plugin/clojure/repl*' -delete
	find target/plugin -type d -empty -delete
	cd target/plugin; jar cmf META-INF/MANIFEST.MF $(plugin_name) plugin.xml amperity clojure com hiccup io less org riemann

plugin: $(plugin_path)

$(install_path): $(plugin_path)
	cp $^ $@
	cd gocd; docker-compose restart server

docker-install: $(install_path)
