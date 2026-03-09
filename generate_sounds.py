import wave, math, struct
import random

sample_rate = 44100

def generate_cash_register():
    duration = 0.5
    num_samples = int(sample_rate * duration)
    with wave.open('app/src/main/res/raw/cash_register.wav', 'w') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(sample_rate)
        
        phase = 0.0
        for i in range(num_samples):
            t = i / sample_rate
            
            # Start with a sharp plink
            if t < 0.1:
                freq = 2500 - 15000 * t
                env = 1.0 - t/0.1
                val = math.sin(freq * 2 * math.pi * t) * env
            # Followed by a metallic clatter
            elif t < 0.4:
                freq = random.uniform(2000, 4000)
                env = math.exp(-15 * (t - 0.1))
                val = math.sin(freq * 2 * math.pi * t) * env
            else:
                val = 0.0
                
            sample = int(max(-1.0, min(1.0, val)) * 32767)
            f.writeframesraw(struct.pack('<h', sample))

def generate_chuckle():
    duration = 1.0
    num_samples = int(sample_rate * duration)
    with wave.open('app/src/main/res/raw/chuckle.wav', 'w') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(sample_rate)
        
        phase = 0.0
        for i in range(num_samples):
            t = i / sample_rate
            
            # Create a rhythmic chuckle "heh heh heh"
            beat = (t * 6) % 1.0
            if beat < 0.3:
                env = math.sin(beat / 0.3 * math.pi)
                freq = 150 + random.uniform(-20, 20)
                val = math.sin(freq * 2 * math.pi * t) * env
                
                # add some grit
                val += random.uniform(-0.1, 0.1) * env
            else:
                val = 0.0
                
            sample = int(max(-1.0, min(1.0, val * 0.5)) * 32767)
            f.writeframesraw(struct.pack('<h', sample))

generate_cash_register()
generate_chuckle()
