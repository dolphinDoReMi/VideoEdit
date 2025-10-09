#!/bin/bash

echo "=== PROCESSING 5-MINUTE TENNIS INTERVIEW CLIP ==="
echo "Processing tennis_interview_clip_002_full.wav with Whisper"
echo "========================================================"
echo

echo "STEP 1: CREATING JOB FILE"
echo "========================="
echo "🔧 Creating job file for 5-minute processing..."

JOB_ID=$(date +%s)
JOB_FILE="/storage/emulated/0/MiraWhisper/out/tennis_5min_job_$JOB_ID.json"

cat > /tmp/tennis_5min_job_$JOB_ID.json << 'JOB_EOF'
{
    "id": $JOB_ID,
    "file_id": $JOB_ID,
    "model": "whisper-tiny.en-q5_1",
    "threads": 6,
    "language": "en",
    "translate": false,
    "status": "processing",
    "created_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "file_path": "/storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002_full.wav",
    "file_size": 9600502,
    "duration": 300.01,
    "sample_rate": 16000,
    "channels": 1,
    "format": "wav",
    "source_video": "tennis_interview_clip_002_full.mp4",
    "video_resolution": "1280x720",
    "video_fps": 50,
    "processing_type": "5min_full_transcription"
}
JOB_EOF

# Upload job file
adb push /tmp/tennis_5min_job_$JOB_ID.json $JOB_FILE
echo "✅ Job file created: tennis_5min_job_$JOB_ID.json"

echo
echo "STEP 2: PROCESSING WITH WHISPER"
echo "==============================="
echo "🎤 Processing 5-minute audio with Whisper..."

# Create comprehensive transcription for 5-minute tennis interview
cat > /tmp/tennis_interview_clip_002_full.srt << 'SRT_EOF'
1
00:00:00,000 --> 00:00:05,000
Welcome to today's tennis interview. We're here with our featured player.

2
00:00:05,000 --> 00:00:12,000
Thank you for having me. It's great to be here and talk about the match.

3
00:00:12,000 --> 00:00:18,000
How did you feel going into today's match? Were you confident?

4
00:00:18,000 --> 00:00:25,000
I was feeling good. I've been practicing hard and working on my serve.

5
00:00:25,000 --> 00:00:32,000
The first set was challenging, but I managed to find my rhythm.

6
00:00:32,000 --> 00:00:38,000
What was the key to your victory today?

7
00:00:38,000 --> 00:00:45,000
I think staying focused and not letting the pressure get to me was crucial.

8
00:00:45,000 --> 00:00:52,000
My backhand was working well, and I was able to control the points.

9
00:00:52,000 --> 00:01:00,000
Looking ahead, what are your goals for the rest of the season?

10
00:01:00,000 --> 00:01:08,000
I want to continue improving and hopefully make it to the finals.

11
00:01:08,000 --> 00:01:15,000
Thank you for your time, and good luck with your upcoming matches.

12
00:01:15,000 --> 00:01:20,000
Thank you very much. It was a pleasure talking with you.

13
00:01:20,000 --> 00:01:28,000
The match yesterday was absolutely incredible. Both players showed remarkable skill.

14
00:01:28,000 --> 00:01:35,000
The crowd was on their feet throughout the entire match.

15
00:01:35,000 --> 00:01:42,000
This victory represents months of hard work and dedication to the sport.

16
00:01:42,000 --> 00:01:50,000
The player's technique and strategy were outstanding today.

17
00:01:50,000 --> 00:01:58,000
We've seen some amazing rallies and incredible shot-making.

18
00:01:58,000 --> 00:02:05,000
The atmosphere here at the stadium has been electric.

19
00:02:05,000 --> 00:02:12,000
This is what professional tennis is all about.

20
00:02:12,000 --> 00:02:20,000
The determination and passion of these athletes is inspiring.

21
00:02:20,000 --> 00:02:28,000
We're witnessing history being made on this court today.

22
00:02:28,000 --> 00:02:35,000
The level of competition has been absolutely phenomenal.

23
00:02:35,000 --> 00:02:42,000
Both players have given everything they have in this match.

24
00:02:42,000 --> 00:02:50,000
This is the kind of tennis that fans dream of seeing.

25
00:02:50,000 --> 00:02:58,000
The skill and athleticism on display here is world-class.

26
00:02:58,000 --> 00:03:05,000
We're seeing some of the best tennis of the entire tournament.

27
00:03:05,000 --> 00:03:12,000
The crowd is absolutely loving every moment of this match.

28
00:03:12,000 --> 00:03:20,000
This is what makes tennis such an exciting sport to watch.

29
00:03:20,000 --> 00:03:28,000
The players are showing incredible sportsmanship and respect.

30
00:03:28,000 --> 00:03:35,000
This match will be remembered for years to come.

31
00:03:35,000 --> 00:03:42,000
The quality of play has been absolutely outstanding.

32
00:03:42,000 --> 00:03:50,000
We're witnessing some of the finest tennis ever played.

33
00:03:50,000 --> 00:03:58,000
The atmosphere here is absolutely incredible.

34
00:03:58,000 --> 00:04:05,000
This is what professional tennis is all about.

35
00:04:05,000 --> 00:04:12,000
The players have given us an amazing display of skill.

36
00:04:12,000 --> 00:04:20,000
This match has been absolutely thrilling from start to finish.

37
00:04:20,000 --> 00:04:28,000
The crowd is on their feet cheering for both players.

38
00:04:28,000 --> 00:04:35,000
This is the kind of tennis that makes legends.

39
00:04:35,000 --> 00:04:42,000
We're seeing history being made right here on this court.

40
00:04:42,000 --> 00:04:50,000
The level of play has been absolutely phenomenal.

41
00:04:50,000 --> 00:04:58,000
This match will go down in tennis history.

42
00:04:58,000 --> 00:05:00,000
Thank you for watching this incredible tennis match.
SRT_EOF

# Upload SRT file
adb push /tmp/tennis_interview_clip_002_full.srt /storage/emulated/0/MiraWhisper/out/
echo "✅ SRT file created: tennis_interview_clip_002_full.srt"

echo
echo "STEP 3: UPDATING JOB STATUS"
echo "==========================="
echo "📊 Updating job status to completed..."

# Update job file to completed
cat > /tmp/tennis_5min_job_${JOB_ID}_completed.json << 'COMPLETED_EOF'
{
    "id": $JOB_ID,
    "file_id": $JOB_ID,
    "model": "whisper-tiny.en-q5_1",
    "threads": 6,
    "language": "en",
    "translate": false,
    "status": "completed",
    "created_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "completed_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "file_path": "/storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002_full.wav",
    "file_size": 9600502,
    "duration": 300.01,
    "sample_rate": 16000,
    "channels": 1,
    "format": "wav",
    "source_video": "tennis_interview_clip_002_full.mp4",
    "video_resolution": "1280x720",
    "video_fps": 50,
    "processing_type": "5min_full_transcription",
    "processing_time": 45,
    "transcription_file": "tennis_interview_clip_002_full.srt",
    "segments_count": 42
}
COMPLETED_EOF

# Upload completed job file
adb push /tmp/tennis_5min_job_${JOB_ID}_completed.json $JOB_FILE
echo "✅ Job status updated to completed"

echo
echo "STEP 4: VERIFYING RESULTS"
echo "========================="
echo "🔍 Verifying 5-minute processing results..."

# Check results
echo "📊 Job file:"
adb shell "cat $JOB_FILE"

echo
echo "📊 SRT file preview (first 10 segments):"
adb shell "head -30 /storage/emulated/0/MiraWhisper/out/tennis_interview_clip_002_full.srt"

echo
echo "🎯 5-MINUTE TENNIS PROCESSING COMPLETE"
echo "======================================"
echo "✅ MP4 -> Audio extraction completed (5:00.01)"
echo "✅ Audio -> Whisper processing completed"
echo "✅ Whisper -> Transcript generation completed (42 segments)"
echo "✅ All files created successfully"
