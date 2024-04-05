#!/bin/bash
SESSION="tmux$$"
tmux -2 new-session -d -s $SESSION
tmux set status-bg default
tmux split-window -v
tmux select-pane -t 0
tmux send-keys "sh scripts/run-liveserver.sh" C-m
tmux select-pane -t 1
tmux send-keys "sh scripts/run-devserver.sh" C-m
tmux -2 attach-session -t $SESSION

