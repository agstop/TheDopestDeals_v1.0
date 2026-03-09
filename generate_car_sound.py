import wave, math, struct
import random

sample_rate = 44100
duration = 2.5
num_samples = int(sample_rate * duration)

with wave.open('app/src/main/res/raw/car_travel.wav', 'w') as f:
    f.setnchannels(1)
    f.setsampwidth(2)
    f.setframerate(sample_rate)
    
    phase = 0.0
    for i in range(num_samples):
        t = i / sample_rate
        
        if t < 1.0:
            freq = 60 + 80 * t
        elif t < 2.0:
            freq = 140 - 100 * (t - 1.0)
        else:
            freq = 40 - 10 * (t - 2.0)
            
        phase += freq * 2 * math.pi / sample_rate
        
        val = 1.0 if math.sin(phase) > 0 else -1.0
        val += 0.5 * math.sin(phase * 2.5)
        
        noise = random.uniform(-0.3, 0.3)
        val += noise
        
        if t < 0.2:
            env = t / 0.2
        elif t > 2.0:
            env = 1.0 - (t - 2.0) / 0.5
        else:
            env = 1.0
            
        if 2.0 < t < 2.3:
            squeal_freq = 2000 - 500 * (t - 2.0)
            squeal = math.sin(squeal_freq * 2 * math.pi * t) * 0.4 * env
            val += squeal
            
        val *= env
        
        sample = int(max(-1.0, min(1.0, val / 2.0)) * 32767)
        f.writeframesraw(struct.pack('<h', sample))
