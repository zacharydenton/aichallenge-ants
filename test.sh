#!/usr/bin/env bash
python tools/playgame.py "java MyBot" "python tools/sample_bots/python/HunterBot.py" --map_file tools/maps/example/tutorial1.map --log_dir game_logs --turns 200 --scenario --food none --player_seed 7 --verbose -e
