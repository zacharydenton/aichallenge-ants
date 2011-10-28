#!/usr/bin/env bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
export PATH=$DIR/../:$PATH
#python $DIR/playgame.py --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file $DIR/maps/random_walk/random_walk_09p_01.map "$@" \
#	"java MyBot" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \
#	"python $DIR/sample_bots/python/LeftyBot.py" \


	#--map_file $DIR/maps/random_walk/random_walk_05p_01.map "$@" \
#python $DIR/playgame.py --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file $DIR/maps/random_walk/random_walk_04p_01.map "$@" \
python $DIR/playgame.py --player_seed 42 --end_wait=0.25 --verbose --log_dir game_logs --turns 1000 --map_file $DIR/maps/maze/maze_02p_02.map "$@" \
	"java MyBot" \
	"python $DIR/sample_bots/python/GreedyBot.py" \
