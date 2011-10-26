#!/usr/bin/env bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
export PATH=$DIR/../:$PATH
python $DIR/playgame.py -So --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file $DIR/maps/maze/maze_07p_01.map "$@" \
	"java MyBot" \
	"java MyBot" \
	"java MyBot" \
	"java MyBot" \
	"java MyBot" \
	"java MyBot" \
	"java MyBot" |
java -jar $DIR/visualizer.jar
