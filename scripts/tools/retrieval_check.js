// node tools/retrieval_check.js store.f32 dim query.json
const fs = require("fs");
function readF32(path, dim){
  const buf = fs.readFileSync(path);
  const dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength);
  const n = buf.length / (4*dim);
  let off = 0, out = [];
  for (let i=0;i<n;i++){ const row = new Float32Array(dim);
    for (let d=0; d<dim; d++, off+=4) row[d]=dv.getFloat32(off,true);
    out.push(row);
  } return out;
}
function l2(a){ let s=0; for(const x of a) s+=x*x; s=Math.sqrt(s); for(let i=0;i<a.length;i++) a[i]/=s; }
function cos(a,b){ let s=0; for(let i=0;i<a.length;i++) s+=a[i]*b[i]; return s; }
const [,, storePath, dimStr, qPath] = process.argv;
const dim = parseInt(dimStr,10);
const store = readF32(storePath, dim);
const q = JSON.parse(fs.readFileSync(qPath,"utf8"));
l2(q);
const scores = store.map((v,i)=>({i,s:cos(q,v)})).sort((a,b)=>b.s-a.s).slice(0,10);
console.log(scores);
