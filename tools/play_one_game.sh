#!/usr/bin/env sh
DIR="$( cd "$( dirname "$0" )" && pwd )"
export PATH=$DIR/../:$PATH
python $DIR/playgame.py --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file $DIR/maps/maze/maze_04p_01.map "$@" \
	"java MyBot" \
	"python $DIR/sample_bots/python/LeftyBot.py" \
	"python $DIR/sample_bots/python/HunterBot.py" \
	"python $DIR/sample_bots/python/LeftyBot.py"
