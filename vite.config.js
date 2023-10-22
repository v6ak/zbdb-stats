import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import * as fs from 'fs';
import { importFresh, sleep } from './vite-funcs';

const scalaJsResourceGen = scalaJSPlugin({
    projectID: 'resourcegen',
    uriPrefix: 'resourcegen',
})

function importResourceGen() {
    const moduleFileName = scalaJsResourceGen.resolveId('resourcegen:main.js');
    return importFresh(moduleFileName)
}

const resourcegenPlugin = {
    name: 'resourcegen',
    configureServer(server) {
        // return a post hook that is called after internal middlewares are installed
        return () => {
            server.middlewares.use(async (req, res, next) => {
                const resourcegenModule = await importResourceGen();    // reload it every time it is changed
                resourcegenModule.Server.handle(req, res, next)
            })
        }
    },
    async load(id) {
        console.log("load", id)
    }
}

export default defineConfig({
    root: 'assets',
    appType: 'mpa',
    cacheDir: 'tmp',
    plugins: [
        // sleep prevents race condition, i.e., SBT fails when started twice at the same time
        sleep(1500).then( () =>
            scalaJSPlugin({
                projectID: 'client',
                uriPrefix: 'client',
            })
        ),
        scalaJsResourceGen,
        resourcegenPlugin,
    ],
    server: {
        host: "127.0.0.1",
        port: 9000,
    },
    build: {
        sourcemap: true,
    }
});
