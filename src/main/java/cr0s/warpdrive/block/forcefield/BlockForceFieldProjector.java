package cr0s.warpdrive.block.forcefield;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;

public class BlockForceFieldProjector extends BlockAbstractForceField {
	@SideOnly(Side.CLIENT)
	private IIcon[] icons;
	
	public BlockForceFieldProjector(final byte tier) {
		super(tier, Material.iron);
		isRotating = true;
		setBlockName("warpdrive.forcefield.projector" + tier);
		setBlockTextureName("warpdrive:forcefield/projector");
	}
	
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		icons = new IIcon[11];
		icons[ 0] = iconRegister.registerIcon("warpdrive:forcefield/projectorSide_notConnected");
		icons[ 1] = iconRegister.registerIcon("warpdrive:forcefield/projectorSide_connectedNotPowered");
		icons[ 2] = iconRegister.registerIcon("warpdrive:forcefield/projectorSide_connectedPowered");
		icons[ 3] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_none");
		icons[ 4] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_sphere");
		icons[ 5] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_cylinder_h");
		icons[ 6] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_cylinder_v");
		icons[ 7] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_cube");
		icons[ 8] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_plane");
		icons[ 9] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_tube");
		icons[10] = iconRegister.registerIcon("warpdrive:forcefield/projectorShape_tunnel");
	}
	
	@Override
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		int metadata = world.getBlockMetadata(x, y, z);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity == null || !(tileEntity instanceof TileEntityForceFieldProjector)) {
			return icons[0];
		}
		
		if (side == (metadata & 7) || (((TileEntityForceFieldProjector)tileEntity).isDoubleSided && ForgeDirection.OPPOSITES[side] == (metadata & 7))) {
			return icons[3 + ((TileEntityForceFieldProjector)tileEntity).getShape().ordinal()];
		} else if (((TileEntityForceFieldProjector)tileEntity).isConnected) {
			if (((TileEntityForceFieldProjector)tileEntity).isPowered) {
				return icons[2];
			} else {
				return icons[1];
			}
		}
		
		return icons[0];
	}
	
	@Override
	public IIcon getIcon(int side, int metadata) {
		return side == 3 ? icons[4] : icons[2];
	}
	
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs creativeTab, List list) {
		for (int i = 0; i < 2; ++i) {
			list.add(new ItemStack(item, 1, i));
		}
	}
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack) {
		super.onBlockPlacedBy(world, x, y, z, entityLiving, itemStack);
		TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector)world.getTileEntity(x, y, z);
		if (!itemStack.hasTagCompound()) {
			tileEntityForceFieldProjector.isDoubleSided = (itemStack.getItemDamage() == 1);
		}
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return false;
		}
		
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityForceFieldProjector)) {
			return false;
		}
		TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector) tileEntity;
		ItemStack itemStackHeld = entityPlayer.getHeldItem();
		int metadata = world.getBlockMetadata(x, y, z);
		
		EnumForceFieldUpgrade enumForceFieldUpgrade = EnumForceFieldUpgrade.NONE;
		if (itemStackHeld != null && itemStackHeld.getItem() instanceof ItemForceFieldUpgrade) {
			enumForceFieldUpgrade = EnumForceFieldUpgrade.get(itemStackHeld.getItemDamage());
		}
		
		// sneaking with an empty hand or an upgrade/shape item in hand to dismount current upgrade/shape
		if (entityPlayer.isSneaking()) {
			// using an upgrade item or no shape defined means dismount upgrade, otherwise dismount shape
			if ((itemStackHeld != null && itemStackHeld.getItem() instanceof ItemForceFieldUpgrade) || tileEntityForceFieldProjector.getShape() == EnumForceFieldShape.NONE) {
				// find a valid upgrade to dismount
				if (!tileEntityForceFieldProjector.hasUpgrade(enumForceFieldUpgrade)) {
					enumForceFieldUpgrade = (EnumForceFieldUpgrade)tileEntityForceFieldProjector.getFirstUpgradeOfType(EnumForceFieldUpgrade.class, EnumForceFieldUpgrade.NONE);
				}
				
				if (enumForceFieldUpgrade == EnumForceFieldUpgrade.NONE) {
					// no more upgrades to dismount
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.result.noUpgradeToDismount"));
					return true;
				}
				
				if (!entityPlayer.capabilities.isCreativeMode) {
					// dismount the current upgrade item
					ItemStack itemStackDrop = ItemForceFieldUpgrade.getItemStackNoCache(enumForceFieldUpgrade, 1);
					EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.delayBeforeCanPickup = 0;
					world.spawnEntityInWorld(entityItem);
				}
				
				tileEntityForceFieldProjector.dismountUpgrade(enumForceFieldUpgrade);
				// upgrade dismounted
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.result.dismounted"));
				return false;
				
			} else {// default to dismount shape
				if (tileEntityForceFieldProjector.getShape() != EnumForceFieldShape.NONE) {
					if (side == (metadata & 7) || (tileEntityForceFieldProjector.isDoubleSided && ForgeDirection.OPPOSITES[side] == (metadata & 7))) {
						if (!entityPlayer.capabilities.isCreativeMode) {
							// dismount the shape item(s)
							ItemStack itemStackDrop = ItemForceFieldShape.getItemStackNoCache(tileEntityForceFieldProjector.getShape(), tileEntityForceFieldProjector.isDoubleSided ? 2 : 1);
							EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
							entityItem.delayBeforeCanPickup = 0;
							world.spawnEntityInWorld(entityItem);
						}
						
						tileEntityForceFieldProjector.setShape(EnumForceFieldShape.NONE);
						// shape dismounted
						WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.result.dismounted"));
					} else {
						// wrong side
						WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.result.wrongSide"));
						return true;
					}
				} else {
					// no shape to dismount
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.result.noShapeToDismount"));
					return true;
				}
			}
			
		} else if (itemStackHeld == null) {// no sneaking and no item in hand => show status
			WarpDrive.addChatMessage(entityPlayer, tileEntityForceFieldProjector.getStatus());
			return true;
			
		} else if (itemStackHeld.getItem() instanceof ItemForceFieldShape) {// no sneaking and shape in hand => mounting a shape
			if (side == (metadata & 7) || (((TileEntityForceFieldProjector) tileEntity).isDoubleSided && ForgeDirection.OPPOSITES[side] == (metadata & 7))) {
				if (!entityPlayer.capabilities.isCreativeMode) {
					// validate quantity
					if (itemStackHeld.stackSize < (tileEntityForceFieldProjector.isDoubleSided ? 2 : 1)) {
						// not enough shape items
						WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted(
							tileEntityForceFieldProjector.isDoubleSided ?
								"warpdrive.forcefield.shape.result.notEnoughShapes.double" : "warpdrive.forcefield.shape.result.notEnoughShapes.single"));
						return true;
					}
					
					// update player inventory
					itemStackHeld.stackSize -= tileEntityForceFieldProjector.isDoubleSided ? 2 : 1;
					
					// dismount the current shape item(s)
					if (tileEntityForceFieldProjector.getShape() != EnumForceFieldShape.NONE) {
						ItemStack itemStackDrop = ItemForceFieldShape.getItemStackNoCache(tileEntityForceFieldProjector.getShape(), tileEntityForceFieldProjector.isDoubleSided ? 2 : 1);
						EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
						entityItem.delayBeforeCanPickup = 0;
						world.spawnEntityInWorld(entityItem);
					}
				}
				
				// mount the new shape item(s)
				tileEntityForceFieldProjector.setShape(EnumForceFieldShape.get(itemStackHeld.getItemDamage()));
				// shape mounted
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.result.mounted"));
				
			} else {
				// wrong side
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.result.wrongSide"));
				return true;
			}
			
		} else if (itemStackHeld.getItem() instanceof ItemForceFieldUpgrade) {// no sneaking and an upgrade in hand => mounting an upgrade
			// validate type
			if (tileEntityForceFieldProjector.getUpgradeMaxCount(enumForceFieldUpgrade) <= 0) {
				// invalid upgrade type
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.result.invalidUpgrade"));
				return true;
			}
			if (!tileEntityForceFieldProjector.canUpgrade(enumForceFieldUpgrade)) {
				// too many upgrades
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.result.tooManyUpgrades",
					tileEntityForceFieldProjector.getUpgradeMaxCount(enumForceFieldUpgrade)));
				return true;
			}
			
			if (!entityPlayer.capabilities.isCreativeMode) {
				// validate quantity
				if (itemStackHeld.stackSize < 1) {
					// not enough upgrade items
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.result.notEnoughUpgrades"));
					return true;
				}
				
				// update player inventory
				itemStackHeld.stackSize -= 1;
			}
			
			// mount the new upgrade item
			tileEntityForceFieldProjector.mountUpgrade(enumForceFieldUpgrade);
			// upgrade mounted
			WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.result.mounted"));
		}
		
		return false;
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileEntityForceFieldProjector();
	}
}