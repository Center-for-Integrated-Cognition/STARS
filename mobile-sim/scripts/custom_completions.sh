#!/bin/bash

# Autocomplete world files in $MOBILE_SIM_HOME/worlds
_mobile_worlds_completion()
{
	local cur=${COMP_WORDS[COMP_CWORD]}
	COMPREPLY=( $(compgen -W "$(ls $MOBILE_SIM_HOME/worlds)" -- $cur) )
}

