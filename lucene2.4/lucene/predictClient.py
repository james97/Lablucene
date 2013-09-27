import barrister

trans  = barrister.HttpTransport("http://localhost:60000/predictor")

# automatically connects to endpoint and loads IDL JSON contract
client = barrister.Client(trans)

# print client.Predictor.predict('1. 2. 3. 4. 5. 6.')
print client.Predictor.predict_id('1. 2. 3. 4. 5. 6.', 450)
print
print "IDL metadata:"
meta = client.get_meta()
for key in [ "barrister_version", "checksum" ]:
    print "%s=%s" % (key, meta[key])

# not printing this one because it changes per run, which breaks our
# very literal 'examples' test harness, but let's verify it exists at least..
assert meta.has_key("date_generated")