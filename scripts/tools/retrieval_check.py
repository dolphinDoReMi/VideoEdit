# python tools/retrieval_check.py store.f32 dim query.json
import sys, struct, json, math
store, dim, qpath = sys.argv[1], int(sys.argv[2]), sys.argv[3]
buf = open(store,'rb').read()
n = len(buf)//(4*dim)
vecs = [list(struct.unpack_from('<'+'f'*dim, buf, i*4*dim)) for i in range(n)]
q = json.load(open(qpath))
s = math.sqrt(sum(x*x for x in q)); q=[x/s for x in q]
def cos(a,b): return sum(x*y for x,y in zip(a,b))
scores = sorted([(i,cos(q,v)) for i,v in enumerate(vecs)], key=lambda x:-x[1])[:10]
print(scores)
