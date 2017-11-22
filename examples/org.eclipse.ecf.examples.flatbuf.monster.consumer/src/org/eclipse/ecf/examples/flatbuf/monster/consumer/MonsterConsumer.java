package org.eclipse.ecf.examples.flatbuf.monster.consumer;

import org.eclipse.ecf.examples.flatbuf.monster.Color;
import org.eclipse.ecf.examples.flatbuf.monster.Equipment;
import org.eclipse.ecf.examples.flatbuf.monster.IScare;
import org.eclipse.ecf.examples.flatbuf.monster.Monster;
import org.eclipse.ecf.examples.flatbuf.monster.Vec3;
import org.eclipse.ecf.examples.flatbuf.monster.Weapon;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.flatbuffers.FlatBufferBuilder;

@Component(immediate = true)
public class MonsterConsumer {

	private IScare scarer;

	@Reference
	void bindScare(IScare s) {
		this.scarer = s;
	}

	void unbindScare(IScare s) {
		this.scarer = null;
	}

	@Activate
	void activate() throws Exception {
		FlatBufferBuilder builder = new FlatBufferBuilder(0);

		// Create some weapons for our Monster ('Sword' and 'Axe').
		int weaponOneName = builder.createString("Sword");
		short weaponOneDamage = 3;
		int weaponTwoName = builder.createString("Axe");
		short weaponTwoDamage = 5;

		// Use the `createWeapon()` helper function to create the weapons, since we set
		// every field.
		int[] weaps = new int[2];
		weaps[0] = Weapon.createWeapon(builder, weaponOneName, weaponOneDamage);
		weaps[1] = Weapon.createWeapon(builder, weaponTwoName, weaponTwoDamage);

		// Serialize the FlatBuffer data.
		int name = builder.createString("Orc");
		byte[] treasure = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		int inv = Monster.createInventoryVector(builder, treasure);
		int weapons = Monster.createWeaponsVector(builder, weaps);
		int pos = Vec3.createVec3(builder, 1.0f, 2.0f, 3.0f);

		Monster.startMonster(builder);
		Monster.addPos(builder, pos);
		Monster.addName(builder, name);
		Monster.addColor(builder, Color.Red);
		Monster.addHp(builder, (short) 300);
		Monster.addInventory(builder, inv);
		Monster.addWeapons(builder, weapons);
		Monster.addEquippedType(builder, Equipment.Weapon);
		Monster.addEquipped(builder, weaps[1]);
		int orc = Monster.endMonster(builder);

		builder.finish(orc);

		Monster retMonster = this.scarer.scareWith(Monster.getRootAsMonster(builder.dataBuffer()));

		System.out.println("retMonster=" + retMonster);
	}
}
