#fix soar output of world
sh typical_replace_handle_bars.sh final-world.txt
sh typical_replace_handle_bars.sh initial-world.txt

#fix data output
sh typical_replace_handle_semicolon.sh used_response.txt
sh typical_replace_handle_semicolon.sh viable.txt
sh typical_replace_handle_semicolon.sh unviable.txt
sh typical_replace_handle_semicolon.sh retrieved.txt
sh typical_replace_handle_semicolon.sh offered.txt

#need to make sure don't replace other stats, comes after prompt
sh typical_replace_handle_tsemicolon.sh tokens.txt


