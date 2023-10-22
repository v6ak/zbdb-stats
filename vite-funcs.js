import * as crypto from 'crypto'
import * as fs from 'fs';


export async function sha256File(name) {
    const hash = crypto.createHash('sha256');
    const content = await fs.promises.readFile(name)
    hash.update(content)
    return hash.digest('hex')
}

export async function importFresh(name){
    const hash = await sha256File(name)
    return await import(name + "?" + hash);
}

export function sleep(time) {
    return new Promise((resolve, reject) =>
        setTimeout(
            () => resolve(null),
            time
        )
    )
}
